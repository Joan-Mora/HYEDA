package co.edu.uniminuto.hyeda.analytics.service;

import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.CustomerRank;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.Dashboard;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.HeldItem;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.IncidentSummary;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.ProductRank;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.RentalDigestRow;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.RevenuePoint;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.StockAlert;
import co.edu.uniminuto.hyeda.documents.service.DocumentService;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import co.edu.uniminuto.hyeda.finance.service.FinanceService;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;

/**
 * Exportacion de reportes en PDF y Excel.
 *
 * <p>Ambos formatos salen de las mismas metricas: el PDF es el que se imprime o
 * se manda por WhatsApp, y el Excel es el que la administradora sigue usando
 * para sus propias cuentas.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM");

    private final AnalyticsService analyticsService;
    private final FinanceService financeService;
    private final DocumentService documentService;

    // ------------------------------------------------------------------- PDF

    /** Reporte analitico del rango, con graficas dibujadas en el propio PDF. */
    @Transactional(readOnly = true)
    public byte[] analyticsPdf(LocalDate from, LocalDate to) {
        Dashboard dashboard = analyticsService.dashboard(from, to);
        String serial = documentService.nextReportSerial();

        Map<String, Object> model = documentService.reportModel(serial, "Reporte de Analiticas");
        model.put("periodLabel", periodLabel(from, to));
        model.put("kpis", dashboard.kpis());
        model.put("billedLabel", Money.format(dashboard.kpis().billed()));
        model.put("incomeLabel", Money.format(dashboard.kpis().income()));
        model.put("expenseLabel", Money.format(dashboard.kpis().expense()));
        model.put("profitLabel", Money.format(dashboard.kpis().profit()));
        model.put("openDebtLabel", Money.format(dashboard.kpis().openDebt()));
        model.put("averageTicketLabel", Money.format(dashboard.kpis().averageTicket()));

        model.put("topProducts", productChart(dashboard.topProducts()));
        model.put("topByCount", customerRows(dashboard.topCustomersByCount()));
        model.put("topByValue", customerRows(dashboard.topCustomersByValue()));
        model.put("series", seriesChart(dashboard.revenueSeries()));
        model.put("incidents", incidentRows(dashboard.incidents()));
        model.put("stockouts", dashboard.stockouts());

        return documentService.render("documents/reporte-analitico", model);
    }

    /**
     * Reporte extenso del dia: lo que se alquilo, que material esta afuera, a
     * nombre de quien y hasta cuando. Es el que recibe la administradora por
     * WhatsApp al cerrar la jornada.
     */
    @Transactional(readOnly = true)
    public byte[] dailyReportPdf(LocalDate date) {
        return operationReportPdf(date, date, "Reporte Diario de Operacion");
    }

    @Transactional(readOnly = true)
    public byte[] operationReportPdf(LocalDate from, LocalDate to, String kind) {
        Dashboard dashboard = analyticsService.dashboard(from, to);
        List<RentalDigestRow> rentals = analyticsService.rentalsIn(from, to);
        List<HeldItem> held = analyticsService.heldItems();
        List<RentalDigestRow> overdue = analyticsService.overdueRentals();

        String serial = documentService.nextReportSerial();
        Map<String, Object> model = documentService.reportModel(serial, kind);
        model.put("periodLabel", periodLabel(from, to));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("rentalsOpened", dashboard.kpis().rentalsOpened());
        summary.put("rentalsClosed", dashboard.kpis().successfulRentals());
        summary.put("incomeLabel", Money.format(dashboard.kpis().income()));
        summary.put("expenseLabel", Money.format(dashboard.kpis().expense()));
        summary.put("profitLabel", Money.format(dashboard.kpis().profit()));
        summary.put("openDebtLabel", Money.format(dashboard.kpis().openDebt()));
        summary.put("unitsOut", dashboard.kpis().unitsOut());
        model.put("summary", summary);

        model.put("rentals", rentals.stream().map(this::digestRow).toList());
        model.put("heldItems", held.stream().map(this::heldRow).toList());
        model.put("overdue", overdue.stream().map(this::digestRow).toList());
        model.put("topProducts", dashboard.topProducts().stream().map(product -> Map.of(
                "name", product.name(),
                "units", product.units(),
                "amountLabel", Money.format(product.amount()))).toList());
        model.put("lowStock", analyticsService.lowStock().stream().map(alert -> Map.of(
                "name", alert.name(),
                "available", alert.available())).toList());

        return documentService.render("documents/reporte-diario", model);
    }

    // ----------------------------------------------------------------- Excel

    /**
     * Libro de Excel con una hoja por metrica. Se generan valores numericos, no
     * texto, para que la administradora pueda seguir haciendo sus formulas.
     */
    @Transactional(readOnly = true)
    public byte[] analyticsExcel(LocalDate from, LocalDate to) {
        Dashboard dashboard = analyticsService.dashboard(from, to);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = headerStyle(workbook);
            CellStyle money = moneyStyle(workbook);

            writeSummary(workbook, header, money, dashboard, from, to);
            writeProducts(workbook, header, money, dashboard.topProducts());
            writeCustomers(workbook, header, money, "Clientes por cantidad",
                    dashboard.topCustomersByCount());
            writeCustomers(workbook, header, money, "Clientes por valor",
                    dashboard.topCustomersByValue());
            writeSeries(workbook, header, money, dashboard.revenueSeries());
            writeRentals(workbook, header, money, analyticsService.rentalsIn(from, to));
            writeHeldItems(workbook, header, analyticsService.heldItems());
            writeStock(workbook, header, dashboard.stockouts());
            writeMovements(workbook, header, money, from, to);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("No se pudo generar el Excel: " + e.getMessage());
        }
    }

    private void writeSummary(Workbook workbook, CellStyle header, CellStyle money,
                              Dashboard dashboard, LocalDate from, LocalDate to) {
        Sheet sheet = workbook.createSheet("Resumen");
        int index = 0;

        Row title = sheet.createRow(index++);
        cell(title, 0, "Periodo", header);
        cell(title, 1, periodLabel(from, to), null);

        index++;
        index = kpiRow(sheet, index, "Alquileres abiertos", dashboard.kpis().rentalsOpened(), header);
        index = kpiRow(sheet, index, "Alquileres cerrados con pago",
                dashboard.kpis().successfulRentals(), header);
        index = kpiRow(sheet, index, "Alquileres en curso", dashboard.kpis().activeRentals(), header);
        index = kpiRow(sheet, index, "Alquileres vencidos", dashboard.kpis().overdueRentals(), header);
        index = kpiRow(sheet, index, "Devoluciones con novedad",
                dashboard.kpis().problemReturns(), header);
        index = kpiRow(sheet, index, "Clientes atendidos", dashboard.kpis().distinctCustomers(), header);
        index = kpiRow(sheet, index, "Unidades en la calle", dashboard.kpis().unitsOut(), header);

        index = moneyRow(sheet, index, "Valor facturado", dashboard.kpis().billed(), header, money);
        index = moneyRow(sheet, index, "Ingresos", dashboard.kpis().income(), header, money);
        index = moneyRow(sheet, index, "Gastos", dashboard.kpis().expense(), header, money);
        index = moneyRow(sheet, index, "Resultado", dashboard.kpis().profit(), header, money);
        index = moneyRow(sheet, index, "Cartera pendiente", dashboard.kpis().openDebt(), header, money);
        moneyRow(sheet, index, "Ticket promedio", dashboard.kpis().averageTicket(), header, money);

        autoSize(sheet, 2);
    }

    private void writeProducts(Workbook workbook, CellStyle header, CellStyle money,
                               List<ProductRank> products) {
        Sheet sheet = workbook.createSheet("Equipos mas alquilados");
        headerRow(sheet, header, "Equipo", "Unidades", "Valor facturado");

        int index = 1;
        for (ProductRank product : products) {
            Row row = sheet.createRow(index++);
            cell(row, 0, product.name(), null);
            cell(row, 1, product.units(), null);
            cell(row, 2, product.amount(), money);
        }
        autoSize(sheet, 3);
    }

    private void writeCustomers(Workbook workbook, CellStyle header, CellStyle money,
                                String sheetName, List<CustomerRank> customers) {
        Sheet sheet = workbook.createSheet(sheetName);
        headerRow(sheet, header, "Cliente", "Documento", "Alquileres", "Valor");

        int index = 1;
        for (CustomerRank customer : customers) {
            Row row = sheet.createRow(index++);
            cell(row, 0, customer.name(), null);
            cell(row, 1, customer.document(), null);
            cell(row, 2, customer.rentals(), null);
            cell(row, 3, customer.amount(), money);
        }
        autoSize(sheet, 4);
    }

    private void writeSeries(Workbook workbook, CellStyle header, CellStyle money,
                             List<RevenuePoint> series) {
        Sheet sheet = workbook.createSheet("Evolucion diaria");
        headerRow(sheet, header, "Fecha", "Facturado", "Ingresos", "Gastos");

        int index = 1;
        for (RevenuePoint point : series) {
            Row row = sheet.createRow(index++);
            cell(row, 0, point.date().format(DATE), null);
            cell(row, 1, point.billed(), money);
            cell(row, 2, point.income(), money);
            cell(row, 3, point.expense(), money);
        }
        autoSize(sheet, 4);
    }

    private void writeRentals(Workbook workbook, CellStyle header, CellStyle money,
                              List<RentalDigestRow> rentals) {
        Sheet sheet = workbook.createSheet("Alquileres");
        headerRow(sheet, header, "Codigo", "Cliente", "Entrega", "Devolucion", "Estado",
                "Dias de atraso", "Valor");

        int index = 1;
        for (RentalDigestRow rental : rentals) {
            Row row = sheet.createRow(index++);
            cell(row, 0, rental.code(), null);
            cell(row, 1, rental.customerName(), null);
            cell(row, 2, rental.start().format(DATE), null);
            cell(row, 3, rental.end().format(DATE), null);
            cell(row, 4, rental.status(), null);
            cell(row, 5, rental.overdueDays(), null);
            cell(row, 6, rental.amount(), money);
        }
        autoSize(sheet, 7);
    }

    private void writeHeldItems(Workbook workbook, CellStyle header, List<HeldItem> held) {
        Sheet sheet = workbook.createSheet("Material en la calle");
        headerRow(sheet, header, "Equipo", "Cantidad", "Cliente", "Alquiler", "Devolver el", "Vencido");

        int index = 1;
        for (HeldItem item : held) {
            Row row = sheet.createRow(index++);
            cell(row, 0, item.productName(), null);
            cell(row, 1, item.quantity(), null);
            cell(row, 2, item.customerName(), null);
            cell(row, 3, item.rentalCode(), null);
            cell(row, 4, item.dueDate().format(DATE), null);
            cell(row, 5, item.overdue() ? "SI" : "NO", null);
        }
        autoSize(sheet, 6);
    }

    private void writeStock(Workbook workbook, CellStyle header, List<StockAlert> alerts) {
        Sheet sheet = workbook.createSheet("Alertas de stock");
        headerRow(sheet, header, "Equipo", "Disponible", "Total", "Estado");

        int index = 1;
        for (StockAlert alert : alerts) {
            Row row = sheet.createRow(index++);
            cell(row, 0, alert.name(), null);
            cell(row, 1, alert.available(), null);
            cell(row, 2, alert.total(), null);
            cell(row, 3, alert.outOfStock() ? "AGOTADO" : "BAJO", null);
        }
        autoSize(sheet, 4);
    }

    private void writeMovements(Workbook workbook, CellStyle header, CellStyle money,
                                LocalDate from, LocalDate to) {
        Sheet sheet = workbook.createSheet("Ingresos y gastos");
        headerRow(sheet, header, "Concepto", "Valor");

        Row income = sheet.createRow(1);
        cell(income, 0, "Total ingresos", null);
        cell(income, 1, financeService.incomeBetween(from, to), money);

        Row expense = sheet.createRow(2);
        cell(expense, 0, "Total gastos", null);
        cell(expense, 1, financeService.expenseBetween(from, to), money);

        autoSize(sheet, 2);
    }

    // ------------------------------------------------------ modelos de grafica

    /** Convierte el ranking en barras porcentuales relativas al primer puesto. */
    private List<Map<String, Object>> productChart(List<ProductRank> products) {
        long max = products.stream().mapToLong(ProductRank::units).max().orElse(0);
        List<Map<String, Object>> rows = new ArrayList<>();

        for (ProductRank product : products) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", product.name());
            row.put("units", product.units());
            row.put("amountLabel", Money.format(product.amount()));
            row.put("percent", max == 0 ? 0 : Math.round(product.units() * 100.0 / max));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> seriesChart(List<RevenuePoint> series) {
        BigDecimal max = series.stream().map(RevenuePoint::billed)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<Map<String, Object>> rows = new ArrayList<>();

        for (RevenuePoint point : series) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dateLabel", point.date().format(SHORT_DATE));
            row.put("billedLabel", Money.format(point.billed()));
            row.put("incomeLabel", Money.format(point.income()));
            row.put("percent", max.signum() == 0 ? 0
                    : point.billed().multiply(BigDecimal.valueOf(100))
                            .divide(max, 0, RoundingMode.HALF_UP).intValue());
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> customerRows(List<CustomerRank> customers) {
        return customers.stream().map(customer -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", customer.name());
            row.put("document", customer.document());
            row.put("rentals", customer.rentals());
            row.put("amountLabel", Money.format(customer.amount()));
            return row;
        }).toList();
    }

    private List<Map<String, Object>> incidentRows(List<IncidentSummary> incidents) {
        return incidents.stream().map(incident -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("condition", incident.condition());
            row.put("count", incident.count());
            row.put("chargedLabel", Money.format(incident.charged()));
            return row;
        }).toList();
    }

    private Map<String, Object> digestRow(RentalDigestRow rental) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("code", rental.code());
        row.put("customerName", rental.customerName());
        row.put("startLabel", rental.start().format(SHORT_DATE));
        row.put("endLabel", rental.end().format(DATE));
        row.put("status", rental.status());
        row.put("amountLabel", Money.format(rental.amount()));
        row.put("overdueDays", rental.overdueDays());
        return row;
    }

    private Map<String, Object> heldRow(HeldItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("productName", item.productName());
        row.put("quantity", item.quantity());
        row.put("customerName", item.customerName());
        row.put("rentalCode", item.rentalCode());
        row.put("dueLabel", item.dueDate().format(DATE));
        return row;
    }

    private String periodLabel(LocalDate from, LocalDate to) {
        return from.equals(to)
                ? "Dia " + from.format(DATE)
                : "Del %s al %s".formatted(from.format(DATE), to.format(DATE));
    }

    // ----------------------------------------------------------- apoyo Excel

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle moneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        return style;
    }

    private void headerRow(Sheet sheet, CellStyle style, String... titles) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            cell(row, i, titles[i], style);
        }
    }

    private int kpiRow(Sheet sheet, int index, String label, long value, CellStyle style) {
        Row row = sheet.createRow(index);
        cell(row, 0, label, style);
        cell(row, 1, value, null);
        return index + 1;
    }

    private int moneyRow(Sheet sheet, int index, String label, BigDecimal value,
                         CellStyle style, CellStyle money) {
        Row row = sheet.createRow(index);
        cell(row, 0, label, style);
        cell(row, 1, value, money);
        return index + 1;
    }

    private void cell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        switch (value) {
            case null -> cell.setBlank();
            case BigDecimal number -> cell.setCellValue(number.doubleValue());
            case Number number -> cell.setCellValue(number.doubleValue());
            default -> cell.setCellValue(String.valueOf(value));
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
