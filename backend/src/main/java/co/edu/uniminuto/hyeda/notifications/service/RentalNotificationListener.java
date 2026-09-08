package co.edu.uniminuto.hyeda.notifications.service;

import co.edu.uniminuto.hyeda.rental.domain.RentalDispatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacciona a los hitos del alquiler encolando los mensajes al cliente.
 *
 * <p>Escucha despues del commit: si la entrega termina revirtiendose, el cliente
 * nunca recibe un resumen de algo que no ocurrio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener
    public void onDispatched(RentalDispatchedEvent event) {
        try {
            notificationService.sendRentalSummary(event.rentalId());
        } catch (RuntimeException e) {
            log.warn("No se pudo encolar el resumen del alquiler {}", event.code(), e);
        }
    }
}
