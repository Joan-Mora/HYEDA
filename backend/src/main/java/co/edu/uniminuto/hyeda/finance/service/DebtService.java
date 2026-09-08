package co.edu.uniminuto.hyeda.finance.service;

import co.edu.uniminuto.hyeda.billing.domain.CustomerCredit;
import co.edu.uniminuto.hyeda.billing.domain.Debt;
import co.edu.uniminuto.hyeda.billing.domain.DebtStatus;
import co.edu.uniminuto.hyeda.billing.repository.CustomerCreditRepository;
import co.edu.uniminuto.hyeda.billing.repository.DebtRepository;
import co.edu.uniminuto.hyeda.crm.domain.Customer;
import co.edu.uniminuto.hyeda.crm.domain.ReputationEventType;
import co.edu.uniminuto.hyeda.crm.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.crm.service.ReputationService;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.AgingBucket;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.CreditView;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.DebtPortfolio;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.DebtRow;
import co.edu.uniminuto.hyeda.rental.domain.Rental;
import co.edu.uniminuto.hyeda.rental.repository.RentalRepository;
import co.edu.uniminuto.hyeda.shared.audit.AuditService;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Cartera por cobrar y saldo a favor.
 *
 * <p>Una deuda nace cuando un alquiler se liquida sin pago completo y se va
 * cerrando con los abonos posteriores. La antiguedad se recalcula a diario:
 * es la que dispara el seguimiento por WhatsApp y castiga la reputacion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DebtService {

    /** A partir de aqui la deuda ya no es un descuido, es mora. */
    public static final int SERIOUS_OVERDUE_DAYS = 15;

    private final DebtRepository debtRepository;
    private final CustomerCreditRepository creditRepository;
    private final CustomerRepository customerRepository;
    private final RentalRepository rentalRepository;
    private final ReputationService reputationService;
    private final AuditService auditService;

    // -------------------------------------------------------------- escritura

    /** Abre o actualiza la deuda de un alquiler liquidado con saldo pendiente. */
    @Transactional
    public Debt openOrUpdate(Rental rental, BigDecimal balance, LocalDate dueDate) {
        Debt debt = debtRepository.findByRentalId(rental.getId()).orElse(null);

        if (debt == null) {
            debt = Debt.builder()
                    .customerId(rental.getCustomerId())
                    .rentalId(rental.getId())
                    .originalAmount(Money.of(balance))
                    .balance(Money.of(balance))
                    .dueDate(dueDate)
                    .status(DebtStatus.ABIERTA)
                    .build();
        } else {
            debt.setBalance(Money.of(balance));
            debt.setDueDate(dueDate);
            debt.setStatus(resolveStatus(debt));
        }
        return debtRepository.save(debt);
    }

    /** Aplica un abono a la deuda del alquiler y la cierra si queda en cero. */
    @Transactional
    public void applyPayment(Long rentalId, BigDecimal amount) {
        debtRepository.findByRentalId(rentalId).ifPresent(debt -> {
            debt.setBalance(Money.atLeastZero(Money.subtract(debt.getBalance(), amount)));
            debt.setStatus(resolveStatus(debt));
            debtRepository.save(debt);

            if (debt.getStatus() == DebtStatus.PAGADA) {
                customerRepository.findById(debt.getCustomerId()).ifPresent(customer ->
                        reputationService.register(customer, ReputationEventType.PAGO_COMPLETO,
                                rentalId, "Deuda cancelada en su totalidad"));
            }
        });
    }

    @Transactional
    public DebtRow forgive(Long debtId, String reason) {
        Debt debt = find(debtId);
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Condonar una deuda exige dejar escrito el motivo");
        }
        debt.setBalance(Money.ZERO);
        debt.setStatus(DebtStatus.CONDONADA);
        auditService.record("CONDONAR_DEUDA", "Debt", debtId, reason);
        return toRow(debtRepository.save(debt));
    }

    @Transactional
    public void markNoticeSent(Long debtId) {
        debtRepository.findById(debtId).ifPresent(debt -> {
            debt.setLastNoticeAt(java.time.LocalDateTime.now());
            debtRepository.save(debt);
        });
    }

    /**
     * Recalcula la antiguedad de toda la cartera. Lo ejecuta el job diario: sin
     * esto, una deuda vencida seguiria apareciendo como si estuviera al dia.
     */
    @Transactional
    public int refreshAging() {
        LocalDate today = LocalDate.now();
        List<Debt> open = debtRepository.findAllOpen();

        for (Debt debt : open) {
            int days = debt.getDueDate() == null ? 0
                    : (int) Math.max(0, ChronoUnit.DAYS.between(debt.getDueDate(), today));
            debt.setOverdueDays(days);
            debt.setStatus(resolveStatus(debt));

            if (days == SERIOUS_OVERDUE_DAYS) {
                // Se castiga una sola vez, el dia que cruza el umbral.
                customerRepository.findById(debt.getCustomerId()).ifPresent(customer ->
                        reputationService.register(customer, ReputationEventType.MORA,
                                debt.getRentalId(),
                                "Deuda con %d dias de vencimiento".formatted(SERIOUS_OVERDUE_DAYS)));
            }
        }
        debtRepository.saveAll(open);
        return open.size();
    }

    // -------------------------------------------------------------- consultas

    @Transactional(readOnly = true)
    public List<DebtRow> openDebts() {
        return debtRepository.findAllOpen().stream().map(this::toRow).toList();
    }

    @Transactional(readOnly = true)
    public List<DebtRow> byCustomer(Long customerId) {
        return debtRepository.findOpenByCustomer(customerId).stream().map(this::toRow).toList();
    }

    @Transactional(readOnly = true)
    public List<Debt> overdueBeyond(int minDays) {
        return debtRepository.findOverdueBeyond(minDays);
    }

    /** Cartera agrupada por antiguedad, como la mira la administradora. */
    @Transactional(readOnly = true)
    public DebtPortfolio portfolio() {
        List<DebtRow> rows = openDebts();
        BigDecimal total = rows.stream().map(DebtRow::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AgingBucket> buckets = new ArrayList<>();
        buckets.add(bucket(rows, "Al dia", 0, 0));
        buckets.add(bucket(rows, "1 a 15 dias", 1, 15));
        buckets.add(bucket(rows, "16 a 30 dias", 16, 30));
        buckets.add(bucket(rows, "31 a 60 dias", 31, 60));
        buckets.add(bucket(rows, "Mas de 60 dias", 61, Integer.MAX_VALUE));

        List<DebtRow> worst = rows.stream()
                .sorted(Comparator.comparingInt(DebtRow::overdueDays).reversed()
                        .thenComparing(DebtRow::balance, Comparator.reverseOrder()))
                .limit(10)
                .toList();

        return new DebtPortfolio(Money.of(total), buckets, worst);
    }

    @Transactional(readOnly = true)
    public List<CreditView> creditsOf(Long customerId) {
        return creditRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(credit -> new CreditView(credit.getId(), credit.getCustomerId(), credit.getAmount(),
                        credit.getReason(), credit.getRentalId(), credit.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal availableCredit(Long customerId) {
        return Money.of(creditRepository.availableCredit(customerId));
    }

    @Transactional
    public CustomerCredit grantCredit(Long customerId, Long rentalId, BigDecimal amount, String reason) {
        return creditRepository.save(CustomerCredit.builder()
                .customerId(customerId)
                .rentalId(rentalId)
                .amount(Money.of(amount))
                .reason(reason)
                .build());
    }

    /** Consume saldo a favor. Se guarda en negativo para que la suma siga siendo el disponible. */
    @Transactional
    public CustomerCredit consumeCredit(Long customerId, Long rentalId, BigDecimal amount, String reason) {
        BigDecimal available = availableCredit(customerId);
        if (Money.gt(amount, available)) {
            throw BusinessException.of("El cliente solo tiene %s de saldo a favor", Money.format(available));
        }
        return creditRepository.save(CustomerCredit.builder()
                .customerId(customerId)
                .rentalId(rentalId)
                .amount(Money.of(amount).negate())
                .reason(reason)
                .build());
    }

    // -------------------------------------------------------------- internos

    private Debt find(Long id) {
        return debtRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Deuda", id));
    }

    private DebtStatus resolveStatus(Debt debt) {
        if (debt.getStatus() == DebtStatus.CONDONADA) {
            return DebtStatus.CONDONADA;
        }
        if (!Money.isPositive(debt.getBalance())) {
            return DebtStatus.PAGADA;
        }
        if (debt.getOverdueDays() > 0) {
            return DebtStatus.VENCIDA;
        }
        return Money.gt(debt.getOriginalAmount(), debt.getBalance())
                ? DebtStatus.PARCIAL
                : DebtStatus.ABIERTA;
    }

    private AgingBucket bucket(List<DebtRow> rows, String label, int min, int max) {
        List<DebtRow> matching = rows.stream()
                .filter(row -> row.overdueDays() >= min && row.overdueDays() <= max)
                .toList();
        BigDecimal amount = matching.stream().map(DebtRow::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AgingBucket(label, matching.size(), Money.of(amount));
    }

    private DebtRow toRow(Debt debt) {
        Customer customer = customerRepository.findById(debt.getCustomerId()).orElse(null);
        String rentalCode = debt.getRentalId() == null ? null
                : rentalRepository.findById(debt.getRentalId()).map(Rental::getCode).orElse(null);

        return new DebtRow(debt.getId(), debt.getCustomerId(),
                customer == null ? null : customer.getName(),
                customer == null ? null : customer.getPhone(),
                debt.getRentalId(), rentalCode, debt.getOriginalAmount(), debt.getBalance(),
                debt.getDueDate(), debt.getOverdueDays(), agingLabel(debt.getOverdueDays()),
                debt.getStatus(), debt.getLastNoticeAt(), debt.getCreatedAt());
    }

    private String agingLabel(int days) {
        if (days <= 0) {
            return "Al dia";
        }
        if (days <= 15) {
            return "1 a 15 dias";
        }
        if (days <= 30) {
            return "16 a 30 dias";
        }
        if (days <= 60) {
            return "31 a 60 dias";
        }
        return "Mas de 60 dias";
    }
}
