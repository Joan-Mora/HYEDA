package co.edu.uniminuto.hyeda.finance.service;

import co.edu.uniminuto.hyeda.billing.repository.CustomerCreditRepository;
import co.edu.uniminuto.hyeda.billing.repository.DebtRepository;
import co.edu.uniminuto.hyeda.finance.domain.FinancialMovement;
import co.edu.uniminuto.hyeda.finance.domain.MovementType;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.CashSummary;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.CategoryTotal;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.DailyPoint;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.MovementRequest;
import co.edu.uniminuto.hyeda.finance.dto.FinanceDtos.MovementView;
import co.edu.uniminuto.hyeda.finance.repository.FinancialMovementRepository;
import co.edu.uniminuto.hyeda.rental.domain.Rental;
import co.edu.uniminuto.hyeda.rental.repository.RentalRepository;
import co.edu.uniminuto.hyeda.shared.audit.AuditService;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.security.CurrentUser;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ingresos y gastos de la empresa.
 *
 * <p>Los pagos de alquiler se asientan aqui de forma automatica desde el modulo
 * de pagos, de modo que la administradora ve un solo libro con lo que entra por
 * alquileres y lo que sale por gastos operativos.
 */
@Service
@RequiredArgsConstructor
public class FinanceService {

    public static final String CATEGORY_RENTAL = "Alquileres";
    public static final String CATEGORY_REFUND = "Devoluciones a clientes";

    private final FinancialMovementRepository repository;
    private final RentalRepository rentalRepository;
    private final DebtRepository debtRepository;
    private final CustomerCreditRepository creditRepository;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    // ---------------------------------------------------------- conciliacion

    /**
     * Asienta un pago en el libro de caja. Es idempotente por {@code paymentId},
     * asi que reintentar la confirmacion de un pago no duplica el ingreso.
     */
    @Transactional
    public void reconcilePayment(Long paymentId, Long rentalId, BigDecimal amount,
                                 MovementType type, String description) {
        if (paymentId != null && repository.existsByPaymentId(paymentId)) {
            return;
        }
        repository.save(FinancialMovement.builder()
                .movementType(type)
                .amount(Money.of(amount))
                .description(description)
                .category(type == MovementType.INGRESO ? CATEGORY_RENTAL : CATEGORY_REFUND)
                .movementDate(LocalDate.now())
                .rentalId(rentalId)
                .paymentId(paymentId)
                .userId(currentUser.idOrNull())
                .build());
    }

    /** Revierte el asiento de un pago anulado. */
    @Transactional
    public void reversePayment(Long paymentId) {
        repository.deleteAll(repository.findByPaymentId(paymentId));
    }

    // ------------------------------------------------------------- movimientos

    @Transactional(readOnly = true)
    public PageResponse<MovementView> search(MovementType type, String category,
                                             LocalDate from, LocalDate to, Pageable pageable) {
        return PageResponse.from(
                repository.search(type, blankToNull(category), from, to, pageable), this::toView);
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return repository.categories();
    }

    @Transactional
    public MovementView create(MovementRequest request) {
        FinancialMovement movement = repository.save(FinancialMovement.builder()
                .movementType(request.movementType())
                .amount(Money.of(request.amount()))
                .description(request.description())
                .category(blankToNull(request.category()))
                .movementDate(request.movementDate())
                .rentalId(request.rentalId())
                .userId(currentUser.idOrNull())
                .build());

        auditService.record("REGISTRAR_MOVIMIENTO", "FinancialMovement", movement.getId(),
                "%s %s".formatted(request.movementType(), Money.format(request.amount())));
        return toView(movement);
    }

    @Transactional
    public MovementView update(Long id, MovementRequest request) {
        FinancialMovement movement = find(id);
        movement.setMovementType(request.movementType());
        movement.setAmount(Money.of(request.amount()));
        movement.setDescription(request.description());
        movement.setCategory(blankToNull(request.category()));
        movement.setMovementDate(request.movementDate());
        movement.setRentalId(request.rentalId());
        return toView(repository.save(movement));
    }

    @Transactional
    public void delete(Long id) {
        FinancialMovement movement = find(id);
        repository.delete(movement);
        auditService.record("ELIMINAR_MOVIMIENTO", "FinancialMovement", id,
                Money.format(movement.getAmount()));
    }

    // ---------------------------------------------------------------- resumen

    @Transactional(readOnly = true)
    public CashSummary summary(LocalDate from, LocalDate to) {
        BigDecimal income = Money.of(repository.totalBetween(MovementType.INGRESO, from, to));
        BigDecimal expense = Money.of(repository.totalBetween(MovementType.GASTO, from, to));

        // Rango anterior de la misma duracion, para saber si el periodo mejoro.
        long span = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate previousTo = from.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(span - 1);
        BigDecimal previousProfit = Money.subtract(
                repository.totalBetween(MovementType.INGRESO, previousFrom, previousTo),
                repository.totalBetween(MovementType.GASTO, previousFrom, previousTo));

        return new CashSummary(from, to, income, expense, Money.subtract(income, expense), previousProfit,
                Money.of(debtRepository.totalOpenBalance()),
                Money.of(creditRepository.totalOutstandingCredit()),
                categoryTotals(MovementType.GASTO, from, to),
                categoryTotals(MovementType.INGRESO, from, to),
                series(from, to));
    }

    @Transactional(readOnly = true)
    public BigDecimal incomeBetween(LocalDate from, LocalDate to) {
        return Money.of(repository.totalBetween(MovementType.INGRESO, from, to));
    }

    @Transactional(readOnly = true)
    public BigDecimal expenseBetween(LocalDate from, LocalDate to) {
        return Money.of(repository.totalBetween(MovementType.GASTO, from, to));
    }

    private List<CategoryTotal> categoryTotals(MovementType type, LocalDate from, LocalDate to) {
        return repository.totalsByCategory(type, from, to).stream()
                .map(row -> new CategoryTotal((String) row[0], Money.of((BigDecimal) row[1])))
                .toList();
    }

    /** Serie diaria completa: los dias sin movimiento se rellenan con cero. */
    private List<DailyPoint> series(LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal[]> byDate = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            byDate.put(day, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Object[] row : repository.dailyTotals(from, to)) {
            LocalDate date = (LocalDate) row[0];
            MovementType type = (MovementType) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            BigDecimal[] slot = byDate.get(date);
            if (slot != null) {
                slot[type == MovementType.INGRESO ? 0 : 1] = amount;
            }
        }

        List<DailyPoint> points = new ArrayList<>();
        byDate.forEach((date, slot) -> points.add(
                new DailyPoint(date, Money.of(slot[0]), Money.of(slot[1]))));
        return points;
    }

    // -------------------------------------------------------------- internos

    private FinancialMovement find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Movimiento", id));
    }

    private MovementView toView(FinancialMovement movement) {
        String rentalCode = movement.getRentalId() == null ? null
                : rentalRepository.findById(movement.getRentalId()).map(Rental::getCode).orElse(null);
        return new MovementView(movement.getId(), movement.getMovementType(), movement.getAmount(),
                movement.getDescription(), movement.getCategory(), movement.getMovementDate(),
                movement.getRentalId(), rentalCode, movement.getPaymentId(),
                movement.getPaymentId() != null, movement.getCreatedAt());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
