package co.edu.uniminuto.hyeda.finance.dto;

import co.edu.uniminuto.hyeda.billing.domain.DebtStatus;
import co.edu.uniminuto.hyeda.finance.domain.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class FinanceDtos {

    private FinanceDtos() {
    }

    public record MovementRequest(
            @NotNull MovementType movementType,
            @NotNull @Positive BigDecimal amount,
            @Size(max = 400) String description,
            @Size(max = 80) String category,
            @NotNull LocalDate movementDate,
            Long rentalId
    ) {
    }

    public record MovementView(
            Long id,
            MovementType movementType,
            BigDecimal amount,
            String description,
            String category,
            LocalDate movementDate,
            Long rentalId,
            String rentalCode,
            Long paymentId,
            boolean automatic,
            LocalDateTime createdAt
    ) {
    }

    /** Resumen de caja de un rango, con la comparacion contra el rango anterior. */
    public record CashSummary(
            LocalDate from,
            LocalDate to,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal profit,
            BigDecimal previousProfit,
            BigDecimal openDebt,
            BigDecimal customerCredit,
            List<CategoryTotal> expensesByCategory,
            List<CategoryTotal> incomeByCategory,
            List<DailyPoint> series
    ) {
    }

    public record CategoryTotal(String category, BigDecimal amount) {
    }

    public record DailyPoint(LocalDate date, BigDecimal income, BigDecimal expense) {
    }

    /** Fila de la cartera. La antiguedad decide el tono del seguimiento. */
    public record DebtRow(
            Long id,
            Long customerId,
            String customerName,
            String customerPhone,
            Long rentalId,
            String rentalCode,
            BigDecimal originalAmount,
            BigDecimal balance,
            LocalDate dueDate,
            int overdueDays,
            String agingBucket,
            DebtStatus status,
            LocalDateTime lastNoticeAt,
            LocalDateTime createdAt
    ) {
    }

    public record DebtPortfolio(
            BigDecimal total,
            List<AgingBucket> buckets,
            List<DebtRow> worstOffenders
    ) {
    }

    public record AgingBucket(String label, int count, BigDecimal amount) {
    }

    public record CreditView(
            Long id,
            Long customerId,
            BigDecimal amount,
            String reason,
            Long rentalId,
            LocalDateTime createdAt
    ) {
    }
}
