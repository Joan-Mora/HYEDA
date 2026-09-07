package co.edu.uniminuto.hyeda.modules.importacion.service;

import co.edu.uniminuto.hyeda.modules.clientes.entity.Customer;
import co.edu.uniminuto.hyeda.modules.clientes.entity.CustomerType;
import co.edu.uniminuto.hyeda.modules.clientes.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.modules.finanzas.entity.FinancialMovement;
import co.edu.uniminuto.hyeda.modules.finanzas.entity.MovementType;
import co.edu.uniminuto.hyeda.modules.finanzas.repository.FinancialMovementRepository;
import co.edu.uniminuto.hyeda.modules.inventario.entity.Product;
import co.edu.uniminuto.hyeda.modules.inventario.repository.ProductRepository;
import co.edu.uniminuto.hyeda.shared.exception.BusinessException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImportService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final FinancialMovementRepository movementRepository;

    public ImportService(ProductRepository productRepository,
                         CustomerRepository customerRepository,
                         FinancialMovementRepository movementRepository) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public Map<String, Object> importProductsFromDocx(MultipartFile file) {
        int created = 0;
        int updated = 0;
        try (InputStream in = file.getInputStream(); XWPFDocument doc = new XWPFDocument(in)) {
            Pattern pricePattern = Pattern.compile("([\\d.]+)\\s*$");
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    if (row.getTableCells().size() < 2) continue;
                    String name = row.getCell(0).getText().trim();
                    String priceText = row.getCell(row.getTableCells().size() - 1).getText().trim()
                            .replace("$", "").replace(".", "").replace(",", ".").trim();
                    if (name.isBlank() || name.equalsIgnoreCase("producto") || name.equalsIgnoreCase("descripcion")) {
                        continue;
                    }
                    BigDecimal price;
                    try {
                        price = new BigDecimal(priceText.replaceAll("[^0-9.]", ""));
                    } catch (Exception e) {
                        Matcher m = pricePattern.matcher(row.getCell(0).getText());
                        if (!m.find()) continue;
                        price = new BigDecimal(m.group(1));
                    }
                    if (price.compareTo(BigDecimal.ZERO) <= 0) continue;
                    var existing = productRepository.findByNameContainingIgnoreCase(name).stream()
                            .filter(p -> p.getName().equalsIgnoreCase(name))
                            .findFirst();
                    if (existing.isPresent()) {
                        Product p = existing.get();
                        p.setDailyPrice(price);
                        productRepository.save(p);
                        updated++;
                    } else {
                        productRepository.save(Product.builder()
                                .name(name)
                                .dailyPrice(price)
                                .stockTotal(0)
                                .stockAvailable(0)
                                .minStockThreshold(1)
                                .active(true)
                                .build());
                        created++;
                    }
                }
            }
            // Also scan paragraphs like "Andamio - $25000"
            Pattern line = Pattern.compile("^(.+?)\\s*[-–]\\s*\\$?([\\d.]+)");
            for (XWPFParagraph p : doc.getParagraphs()) {
                Matcher m = line.matcher(p.getText().trim());
                if (!m.find()) continue;
                String name = m.group(1).trim();
                BigDecimal price = new BigDecimal(m.group(2).replace(".", ""));
                if (productRepository.findByNameContainingIgnoreCase(name).stream()
                        .noneMatch(x -> x.getName().equalsIgnoreCase(name))) {
                    productRepository.save(Product.builder()
                            .name(name).dailyPrice(price).stockTotal(1).stockAvailable(1)
                            .minStockThreshold(1).active(true).build());
                    created++;
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Error importando DOCX: " + e.getMessage());
        }
        return Map.of("created", created, "updated", updated);
    }

    @Transactional
    public Map<String, Object> importInventoryFromExcel(MultipartFile file) {
        int updated = 0;
        int created = 0;
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                String name = cellString(row.getCell(0));
                if (name.isBlank()) continue;
                int qty = (int) cellNumber(row.getCell(1));
                BigDecimal price = BigDecimal.valueOf(cellNumber(row.getCell(2)));
                var existing = productRepository.findByNameContainingIgnoreCase(name).stream()
                        .filter(p -> p.getName().equalsIgnoreCase(name)).findFirst();
                if (existing.isPresent()) {
                    Product p = existing.get();
                    p.setStockTotal(qty);
                    p.setStockAvailable(qty);
                    if (price.compareTo(BigDecimal.ZERO) > 0) p.setDailyPrice(price);
                    productRepository.save(p);
                    updated++;
                } else {
                    productRepository.save(Product.builder()
                            .name(name)
                            .dailyPrice(price.compareTo(BigDecimal.ZERO) > 0 ? price : BigDecimal.TEN)
                            .stockTotal(qty)
                            .stockAvailable(qty)
                            .minStockThreshold(1)
                            .active(true)
                            .build());
                    created++;
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Error importando inventario: " + e.getMessage());
        }
        return Map.of("created", created, "updated", updated);
    }

    @Transactional
    public Map<String, Object> importCustomersFromExcel(MultipartFile file) {
        int created = 0;
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                String document = cellString(row.getCell(0));
                String name = cellString(row.getCell(1));
                if (document.isBlank() || name.isBlank()) continue;
                if (customerRepository.findByDocument(document).isPresent()) continue;
                customerRepository.save(Customer.builder()
                        .document(document)
                        .name(name)
                        .phone(cellString(row.getCell(2)))
                        .address(cellString(row.getCell(3)))
                        .dataConsent(true)
                        .customerType(CustomerType.FRECUENTE)
                        .reputationScore(BigDecimal.valueOf(60))
                        .build());
                created++;
            }
        } catch (Exception e) {
            throw new BusinessException("Error importando clientes: " + e.getMessage());
        }
        return Map.of("created", created);
    }

    @Transactional
    public Map<String, Object> importMovementsFromExcel(MultipartFile file, MovementType type) {
        int created = 0;
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                String desc = cellString(row.getCell(0));
                double amount = cellNumber(row.getCell(1));
                if (desc.isBlank() || amount == 0) continue;
                LocalDate date = cellDate(row.getCell(2));
                movementRepository.save(FinancialMovement.builder()
                        .movementType(type)
                        .amount(BigDecimal.valueOf(Math.abs(amount)))
                        .description(desc)
                        .category(cellString(row.getCell(3)))
                        .movementDate(date == null ? LocalDate.now() : date)
                        .build());
                created++;
            }
        } catch (Exception e) {
            throw new BusinessException("Error importando movimientos: " + e.getMessage());
        }
        return Map.of("created", created, "type", type.name());
    }

    @Transactional
    public Map<String, Object> importAllSeed(MultipartFile products,
                                             MultipartFile inventory,
                                             MultipartFile customers,
                                             MultipartFile ingresos,
                                             MultipartFile gastos) {
        Map<String, Object> result = new HashMap<>();
        if (products != null && !products.isEmpty()) result.put("products", importProductsFromDocx(products));
        if (inventory != null && !inventory.isEmpty()) result.put("inventory", importInventoryFromExcel(inventory));
        if (customers != null && !customers.isEmpty()) result.put("customers", importCustomersFromExcel(customers));
        if (ingresos != null && !ingresos.isEmpty()) result.put("ingresos", importMovementsFromExcel(ingresos, MovementType.INGRESO));
        if (gastos != null && !gastos.isEmpty()) result.put("gastos", importMovementsFromExcel(gastos, MovementType.GASTO));
        return result;
    }

    private String cellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private double cellNumber(Cell cell) {
        if (cell == null) return 0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().replace("$", "").replace(".", "").replace(",", ".").trim());
                } catch (Exception e) {
                    yield 0;
                }
            }
            default -> 0;
        };
    }

    private LocalDate cellDate(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            if (cell.getCellType() == CellType.STRING) {
                return LocalDate.parse(cell.getStringCellValue().trim().substring(0, 10));
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
