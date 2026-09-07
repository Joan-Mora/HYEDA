package co.edu.uniminuto.hyeda.modules.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardDto(
        long rentalsActive,
        long rentalsClosed,
        long lowStockProducts,
        long openDebts,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        List<Map<String, Object>> topProducts,
        List<Map<String, Object>> topCustomersByCount,
        List<Map<String, Object>> topCustomersByValue,
        List<Map<String, Object>> incomeByDay
) {}
