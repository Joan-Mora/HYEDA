package co.edu.uniminuto.hyeda.modules.analytics.service;

import co.edu.uniminuto.hyeda.modules.alquileres.entity.Rental;
import co.edu.uniminuto.hyeda.modules.alquileres.entity.RentalItem;
import co.edu.uniminuto.hyeda.modules.alquileres.entity.RentalStatus;
import co.edu.uniminuto.hyeda.modules.alquileres.repository.RentalRepository;
import co.edu.uniminuto.hyeda.modules.analytics.dto.DashboardDto;
import co.edu.uniminuto.hyeda.modules.documentos.service.DocumentService;
import co.edu.uniminuto.hyeda.modules.finanzas.entity.DebtStatus;
import co.edu.uniminuto.hyeda.modules.finanzas.entity.FinancialMovement;
import co.edu.uniminuto.hyeda.modules.finanzas.entity.MovementType;
import co.edu.uniminuto.hyeda.modules.finanzas.repository.DebtRepository;
import co.edu.uniminuto.hyeda.modules.finanzas.repository.FinancialMovementRepository;
import co.edu.uniminuto.hyeda.modules.inventario.repository.ProductRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final RentalRepository rentalRepository;
    private final ProductRepository productRepository;
    private final DebtRepository debtRepository;
    private final FinancialMovementRepository movementRepository;
    private final DocumentService documentService;

    public AnalyticsService(RentalRepository rentalRepository,
                            ProductRepository productRepository,
                            DebtRepository debtRepository,
                            FinancialMovementRepository movementRepository,
                            DocumentService documentService) {
        this.rentalRepository = rentalRepository;
        this.productRepository = productRepository;
        this.debtRepository = debtRepository;
        this.movementRepository = movementRepository;
        this.documentService = documentService;
    }

    @Transactional(readOnly = true)
    public DashboardDto dashboard(LocalDate from, LocalDate to) {
        List<Rental> rentals = rentalRepository.findAllWithDetails();
        long active = rentals.stream().filter(r -> r.getStatus() == RentalStatus.EN_CURSO).count();
        long closed = rentals.stream().filter(r -> r.getStatus() == RentalStatus.CERRADO).count();

        Map<String, Long> productCounts = new HashMap<>();
        Map<String, BigDecimal> customerValue = new HashMap<>();
        Map<String, Long> customerCount = new HashMap<>();
        for (Rental r : rentals) {
            String customerName = r.getCustomer() != null ? r.getCustomer().getName() : "N/A";
            customerCount.merge(customerName, 1L, (a, b) -> a + b);
            BigDecimal amt = r.getFinalAmount() != null ? r.getFinalAmount() : r.getTotalAmount();
            customerValue.merge(customerName, amt == null ? BigDecimal.ZERO : amt, (a, b) -> a.add(b));
            if (r.getItems() != null) {
                for (RentalItem item : r.getItems()) {
                    String productName = item.getProduct() != null ? item.getProduct().getName() : "N/A";
                    productCounts.merge(productName, (long) item.getQuantity(), (a, b) -> a + b);
                }
            }
        }

        List<FinancialMovement> movements = (from != null && to != null)
                ? movementRepository.findByMovementDateBetween(from, to)
                : movementRepository.findAll();
        BigDecimal income = movements.stream().filter(m -> m.getMovementType() == MovementType.INGRESO)
                .map(m -> m.getAmount()).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        BigDecimal expense = movements.stream().filter(m -> m.getMovementType() == MovementType.GASTO)
                .map(m -> m.getAmount()).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        Map<LocalDate, BigDecimal> byDay = movements.stream()
                .filter(m -> m.getMovementType() == MovementType.INGRESO && m.getMovementDate() != null)
                .collect(Collectors.groupingBy(m -> m.getMovementDate(),
                        Collectors.mapping(m -> m.getAmount(),
                                Collectors.reducing(BigDecimal.ZERO, (a, b) -> a.add(b)))));

        return new DashboardDto(
                active,
                closed,
                productRepository.findLowStock().size(),
                debtRepository.findByStatus(DebtStatus.ABIERTA).size()
                        + debtRepository.findByStatus(DebtStatus.VENCIDA).size(),
                income,
                expense,
                top(productCounts, 10),
                top(customerCount, 10),
                topDecimal(customerValue, 10),
                byDay.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> Map.<String, Object>of("date", e.getKey().toString(), "amount", e.getValue()))
                        .toList()
        );
    }

    public byte[] exportExcel(LocalDate from, LocalDate to) {
        DashboardDto dash = dashboard(from, to);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("KPIs");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Metrica");
            h.createCell(1).setCellValue("Valor");
            sheet.createRow(1).createCell(0).setCellValue("Alquileres activos");
            sheet.getRow(1).createCell(1).setCellValue(dash.rentalsActive());
            sheet.createRow(2).createCell(0).setCellValue("Alquileres cerrados");
            sheet.getRow(2).createCell(1).setCellValue(dash.rentalsClosed());
            sheet.createRow(3).createCell(0).setCellValue("Ingresos");
            sheet.getRow(3).createCell(1).setCellValue(dash.incomeTotal().doubleValue());
            sheet.createRow(4).createCell(0).setCellValue("Gastos");
            sheet.getRow(4).createCell(1).setCellValue(dash.expenseTotal().doubleValue());
            Sheet products = wb.createSheet("TopProductos");
            int i = 0;
            for (Map<String, Object> row : dash.topProducts()) {
                Row r = products.createRow(i++);
                r.createCell(0).setCellValue(String.valueOf(row.get("name")));
                r.createCell(1).setCellValue(Double.parseDouble(String.valueOf(row.get("value"))));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void generateDailyPdfReport() {
        DashboardDto dash = dashboard(LocalDate.now().minusDays(1), LocalDate.now());
        String html = "<html><body><h1>Reporte diario HYEDA</h1>"
                + "<p>Activos: " + dash.rentalsActive() + "</p>"
                + "<p>Cerrados: " + dash.rentalsClosed() + "</p>"
                + "<p>Ingresos: " + dash.incomeTotal() + "</p>"
                + "<p>Deudas abiertas: " + dash.openDebts() + "</p>"
                + "</body></html>";
        documentService.generateReport("Reporte diario", html);
    }

    private List<Map<String, Object>> top(Map<String, Long> map, int n) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
                .toList();
    }

    private List<Map<String, Object>> topDecimal(Map<String, BigDecimal> map, int n) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(n)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
                .toList();
    }
}
