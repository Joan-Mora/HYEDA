package co.edu.uniminuto.hyeda.crm.service;

import co.edu.uniminuto.hyeda.crm.domain.Customer;
import co.edu.uniminuto.hyeda.crm.domain.CustomerType;
import co.edu.uniminuto.hyeda.crm.domain.ReputationEvent;
import co.edu.uniminuto.hyeda.crm.domain.ReputationEventType;
import co.edu.uniminuto.hyeda.crm.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.crm.repository.ReputationEventRepository;
import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Medidor de reputacion del cliente.
 *
 * <p>El puntaje se mueve entre 0 y 100 partiendo de 50. Sube al devolver a
 * tiempo y pagar completo; baja con mora, danos y faltantes. La clasificacion
 * ({@link CustomerType}) se recalcula en cada evento y es la que decide si al
 * cliente se le puede hacer solo remision o si necesita contrato.
 */
@Service
@RequiredArgsConstructor
public class ReputationService {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = new BigDecimal("100");
    private static final BigDecimal RESTRICTED_THRESHOLD = new BigDecimal("30");

    private final CustomerRepository customerRepository;
    private final ReputationEventRepository eventRepository;
    private final HyedaProperties properties;

    @Transactional
    public Customer register(Customer customer, ReputationEventType type, Long rentalId, String reason) {
        return register(customer, type, type.defaultDelta(), rentalId, reason);
    }

    @Transactional
    public Customer register(Customer customer, ReputationEventType type, BigDecimal delta,
                             Long rentalId, String reason) {
        BigDecimal newScore = customer.getReputationScore().add(delta)
                .max(MIN_SCORE)
                .min(MAX_SCORE);

        customer.setReputationScore(newScore);
        customer.setCustomerType(classify(customer));
        Customer saved = customerRepository.save(customer);

        eventRepository.save(ReputationEvent.builder()
                .customerId(saved.getId())
                .rentalId(rentalId)
                .eventType(type)
                .delta(delta)
                .scoreAfter(newScore)
                .reason(reason)
                .build());

        return saved;
    }

    /** Suma un alquiler cerrado con exito al historial del cliente. */
    @Transactional
    public Customer registerCompletedRental(Customer customer, Long rentalId, boolean onTime) {
        customer.setRentalCount(customer.getRentalCount() + 1);
        return register(customer,
                onTime ? ReputationEventType.DEVOLUCION_A_TIEMPO : ReputationEventType.MORA,
                rentalId,
                onTime ? "Alquiler devuelto dentro del plazo" : "Alquiler devuelto fuera de plazo");
    }

    @Transactional(readOnly = true)
    public List<ReputationEvent> history(Long customerId) {
        return eventRepository.findTop20ByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Reglas de clasificacion, en orden de prioridad:
     * un puntaje muy bajo restringe sin importar el historial; para llegar a
     * CONFIANZA hacen falta suficientes alquileres y buen puntaje.
     */
    public CustomerType classify(Customer customer) {
        BigDecimal score = customer.getReputationScore();
        int rentals = customer.getRentalCount() == null ? 0 : customer.getRentalCount();

        if (score.compareTo(RESTRICTED_THRESHOLD) < 0) {
            return CustomerType.RESTRINGIDO;
        }
        HyedaProperties.Rental config = properties.getRental();
        if (rentals >= config.getTrustedMinRentals()
                && score.compareTo(config.getTrustedMinReputation()) >= 0) {
            return CustomerType.CONFIANZA;
        }
        if (rentals >= config.getTrustedMinRentals()) {
            return CustomerType.FRECUENTE;
        }
        return CustomerType.NUEVO;
    }
}
