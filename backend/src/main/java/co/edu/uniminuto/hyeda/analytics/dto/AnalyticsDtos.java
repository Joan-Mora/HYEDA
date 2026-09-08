package co.edu.uniminuto.hyeda.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    /** Granularidad del calendario con la que se pide un reporte. */
    public enum RangePreset {
        DIA,
        SEMANA,
        MES,
        ANIO,
        PERSONALIZADO
    }

    /**
     * Tablero completo de un rango. Se devuelve entero en una sola llamada para
     * que la pantalla no tenga que orquestar diez peticiones distintas.
     */
    public record Dashboard(
            LocalDate from,
            LocalDate to,
            Kpis kpis,
            List<ProductRank> topProducts,
            List<CustomerRank> topCustomersByCount,
            List<CustomerRank> topCustomersByValue,
            List<StockAlert> stockouts,
            List<IncidentSummary> incidents,
            List<RevenuePoint> revenueSeries,
            List<StatusCount> rentalsByStatus
    ) {
    }

    /**
     * @param successfulRentals alquileres cerrados con pago completo
     * @param problemReturns    devoluciones con dano, faltante o inconformidad
     */
    public record Kpis(
            int rentalsOpened,
            int successfulRentals,
            int activeRentals,
            int overdueRentals,
            int problemReturns,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal profit,
            BigDecimal billed,
            BigDecimal openDebt,
            BigDecimal averageTicket,
            int unitsOut,
            int distinctCustomers
    ) {
    }

    public record ProductRank(Long productId, String name, long units, BigDecimal amount) {
    }

    public record CustomerRank(Long customerId, String name, String document, long rentals,
                               BigDecimal amount) {
    }

    public record StockAlert(Long productId, String name, int available, int total, boolean outOfStock) {
    }

    public record IncidentSummary(String condition, long count, BigDecimal charged) {
    }

    public record RevenuePoint(LocalDate date, BigDecimal billed, BigDecimal income, BigDecimal expense) {
    }

    public record StatusCount(String status, long count) {
    }

    /** Fila del reporte de material en poder de clientes. */
    public record HeldItem(String productName, int quantity, String customerName, String rentalCode,
                           LocalDate dueDate, boolean overdue) {
    }

    public record RentalDigestRow(String code, String customerName, LocalDate start, LocalDate end,
                                  String status, BigDecimal amount, int overdueDays) {
    }
}
