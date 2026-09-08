package co.edu.uniminuto.hyeda.imports.service;

import co.edu.uniminuto.hyeda.billing.domain.Debt;
import co.edu.uniminuto.hyeda.billing.domain.DebtStatus;
import co.edu.uniminuto.hyeda.billing.repository.DebtRepository;
import co.edu.uniminuto.hyeda.catalog.domain.Product;
import co.edu.uniminuto.hyeda.catalog.repository.ProductRepository;
import co.edu.uniminuto.hyeda.catalog.service.StockService;
import co.edu.uniminuto.hyeda.crm.domain.Customer;
import co.edu.uniminuto.hyeda.crm.domain.CustomerAddress;
import co.edu.uniminuto.hyeda.crm.domain.IdDocumentType;
import co.edu.uniminuto.hyeda.crm.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.finance.domain.FinancialMovement;
import co.edu.uniminuto.hyeda.finance.domain.MovementType;
import co.edu.uniminuto.hyeda.finance.repository.FinancialMovementRepository;
import co.edu.uniminuto.hyeda.imports.domain.ImportJob;
import co.edu.uniminuto.hyeda.imports.domain.ImportJobStatus;
import co.edu.uniminuto.hyeda.imports.domain.ImportJobType;
import co.edu.uniminuto.hyeda.imports.dto.ImportDtos.ImportJobView;
import co.edu.uniminuto.hyeda.imports.dto.ImportDtos.PreviewResponse;
import co.edu.uniminuto.hyeda.imports.dto.ImportDtos.PreviewRow;
import co.edu.uniminuto.hyeda.imports.repository.ImportJobRepository;
import co.edu.uniminuto.hyeda.rental.repository.RentalRepository;
import co.edu.uniminuto.hyeda.shared.audit.AuditService;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.security.CurrentUser;
import co.edu.uniminuto.hyeda.shared.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ImportService {

    private static final DateTimeFormatter[] DATES = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("d/M/uu")
    };

    private final SpreadsheetParser parser;
    private final ImportJobRepository jobRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final CustomerRepository customerRepository;
    private final FinancialMovementRepository movementRepository;
    private final DebtRepository debtRepository;
    private final RentalRepository rentalRepository;
    private final FileStorageService fileStorage;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    public ImportService(SpreadsheetParser parser,
                         ImportJobRepository jobRepository,
                         ProductRepository productRepository,
                         StockService stockService,
                         CustomerRepository customerRepository,
                         FinancialMovementRepository movementRepository,
                         DebtRepository debtRepository,
                         RentalRepository rentalRepository,
                         FileStorageService fileStorage,
                         AuditService auditService,
                         CurrentUser currentUser) {
        this.parser = parser;
        this.jobRepository = jobRepository;
        this.productRepository = productRepository;
        this.stockService = stockService;
        this.customerRepository = customerRepository;
        this.movementRepository = movementRepository;
        this.debtRepository = debtRepository;
        this.rentalRepository = rentalRepository;
        this.fileStorage = fileStorage;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public PreviewResponse preview(MultipartFile file, ImportJobType type, Map<String, String> mappingOverride) {
        SpreadsheetParser.SheetData sheet = parser.parse(file);
        Map<String, String> mapping = mappingOverride == null || mappingOverride.isEmpty()
                ? ColumnAliases.suggest(type, sheet.headers())
                : mappingOverride;

        List<PreviewRow> preview = new ArrayList<>();
        int valid = 0;
        int errors = 0;
        for (int i = 0; i < sheet.rows().size(); i++) {
            Map<String, String> raw = sheet.rows().get(i);
            Map<String, String> values = applyMapping(raw, mapping);
            List<String> rowErrors = validate(type, values, false);
            if (rowErrors.isEmpty()) {
                valid++;
            } else {
                errors++;
            }
            if (i < 40) {
                preview.add(new PreviewRow(i + 2, values, rowErrors));
            }
        }
        return new PreviewResponse(type, file.getOriginalFilename(), sheet.format(), sheet.headers(),
                mapping, ColumnAliases.requiredFields(type), preview, sheet.rows().size(), valid, errors);
    }

    @Transactional
    public ImportJobView execute(MultipartFile file, ImportJobType type, Map<String, String> mappingOverride) {
        SpreadsheetParser.SheetData sheet = parser.parse(file);
        Map<String, String> mapping = mappingOverride == null || mappingOverride.isEmpty()
                ? ColumnAliases.suggest(type, sheet.headers())
                : mappingOverride;

        ImportJob job = jobRepository.save(ImportJob.builder()
                .jobType(type)
                .fileName(file.getOriginalFilename())
                .format(sheet.format())
                .status(ImportJobStatus.PENDIENTE)
                .rowsTotal(sheet.rows().size())
                .startedBy(currentUser.idOrNull())
                .build());

        int ok = 0;
        List<String> errorLines = new ArrayList<>();
        errorLines.add("fila,campo,error");

        for (int i = 0; i < sheet.rows().size(); i++) {
            int excelRow = i + 2;
            Map<String, String> values = applyMapping(sheet.rows().get(i), mapping);
            List<String> rowErrors = validate(type, values, true);
            if (!rowErrors.isEmpty()) {
                for (String error : rowErrors) {
                    errorLines.add("%d,,\"%s\"".formatted(excelRow, error.replace("\"", "'")));
                }
                continue;
            }
            try {
                persist(type, values);
                ok++;
            } catch (Exception e) {
                errorLines.add("%d,,\"%s\"".formatted(excelRow, e.getMessage().replace("\"", "'")));
            }
        }

        int failed = sheet.rows().size() - ok;
        job.setRowsOk(ok);
        job.setRowsError(failed);
        job.setFinishedAt(LocalDateTime.now());
        if (failed == 0) {
            job.setStatus(ImportJobStatus.COMPLETADO);
            job.setErrorLog(ok + " filas importadas");
        } else if (ok == 0) {
            job.setStatus(ImportJobStatus.FALLIDO);
            job.setErrorLog(storeErrorCsv(errorLines, failed));
        } else {
            job.setStatus(ImportJobStatus.PARCIAL);
            job.setErrorLog(storeErrorCsv(errorLines, failed));
        }

        ImportJob saved = jobRepository.save(job);
        auditService.record("IMPORTAR_" + type.name(), "ImportJob", saved.getId(),
                "%d ok / %d error".formatted(ok, failed));
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public List<ImportJobView> list() {
        return jobRepository.findAllByOrderByStartedAtDesc().stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public ImportJobView find(Long id) {
        return toView(jobRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Importacion", id)));
    }

    @Transactional(readOnly = true)
    public byte[] errorReport(Long id) {
        ImportJob job = jobRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Importacion", id));
        String path = errorPath(job.getErrorLog());
        if (path == null) {
            throw new BusinessException("Esta importacion no tiene reporte de errores");
        }
        return fileStorage.read(path);
    }

    private void persist(ImportJobType type, Map<String, String> values) {
        switch (type) {
            case PRODUCTOS -> persistProduct(values);
            case CLIENTES -> persistCustomer(values);
            case MOVIMIENTOS -> persistMovement(values);
            case DEUDAS -> persistDebt(values);
        }
    }

    private void persistProduct(Map<String, String> values) {
        String sku = blankToNull(values.get("sku"));
        if (sku != null) {
            sku = sku.toUpperCase(Locale.ROOT);
        }
        Product existing = sku == null ? null : productRepository.findBySkuIgnoreCase(sku).orElse(null);
        int stock = parseInt(values.get("stockTotal"), 0);

        if (existing == null) {
            Product product = Product.builder()
                    .sku(sku)
                    .name(values.get("name").trim())
                    .description(blankToNull(values.get("description")))
                    .category(blankToNull(values.get("category")))
                    .unit(blankToNull(values.get("unit")) == null ? "UNIDAD" : values.get("unit").trim())
                    .dailyPrice(parseMoney(values.get("dailyPrice")))
                    .lateFeePerDay(parseMoneyOr(values.get("lateFeePerDay"), parseMoney(values.get("dailyPrice"))))
                    .replacementValue(parseMoney(values.get("replacementValue")))
                    .stockTotal(stock)
                    .stockAvailable(stock)
                    .minStockThreshold(parseInt(values.get("minStockThreshold"), 1))
                    .build();
            Product saved = productRepository.save(product);
            if (stock > 0) {
                stockService.registerImport(saved, stock, "Importacion masiva");
            }
            return;
        }

        existing.setName(values.get("name").trim());
        if (values.get("description") != null && !values.get("description").isBlank()) {
            existing.setDescription(values.get("description").trim());
        }
        if (values.get("category") != null && !values.get("category").isBlank()) {
            existing.setCategory(values.get("category").trim());
        }
        existing.setDailyPrice(parseMoney(values.get("dailyPrice")));
        existing.setLateFeePerDay(parseMoneyOr(values.get("lateFeePerDay"), existing.getDailyPrice()));
        if (values.get("replacementValue") != null && !values.get("replacementValue").isBlank()) {
            existing.setReplacementValue(parseMoney(values.get("replacementValue")));
        }
        productRepository.save(existing);
        if (stock != existing.getStockTotal()) {
            stockService.adjust(existing.getId(), stock, "Ajuste por importacion");
        }
    }

    private void persistCustomer(Map<String, String> values) {
        String document = values.get("document").trim();
        Customer existing = customerRepository.findByDocument(document).orElse(null);
        IdDocumentType documentType = parseDocumentType(values.get("documentType"));

        if (existing == null) {
            Customer customer = Customer.builder()
                    .document(document)
                    .documentType(documentType)
                    .name(values.get("name").trim())
                    .phone(blankToNull(values.get("phone")))
                    .email(blankToNull(values.get("email")))
                    .notes(blankToNull(values.get("notes")))
                    .dataConsent(true)
                    .dataConsentAt(LocalDateTime.now())
                    .build();
            addAddress(customer, values);
            customerRepository.save(customer);
            return;
        }

        existing.setName(values.get("name").trim());
        existing.setDocumentType(documentType);
        if (values.get("phone") != null && !values.get("phone").isBlank()) {
            existing.setPhone(values.get("phone").trim());
        }
        if (values.get("email") != null && !values.get("email").isBlank()) {
            existing.setEmail(values.get("email").trim());
        }
        if (values.get("notes") != null && !values.get("notes").isBlank()) {
            existing.setNotes(values.get("notes").trim());
        }
        if (Boolean.FALSE.equals(existing.getDataConsent())) {
            existing.setDataConsent(true);
            existing.setDataConsentAt(LocalDateTime.now());
        }
        addAddress(existing, values);
        customerRepository.save(existing);
    }

    private void addAddress(Customer customer, Map<String, String> values) {
        String address = blankToNull(values.get("address"));
        if (address == null) {
            return;
        }
        boolean already = customer.getAddresses().stream()
                .anyMatch(item -> address.equalsIgnoreCase(item.getAddress()));
        if (already) {
            return;
        }
        customer.getAddresses().add(CustomerAddress.builder()
                .label("OBRA")
                .address(address)
                .reference(blankToNull(values.get("reference")))
                .isDefault(customer.getAddresses().isEmpty())
                .build());
    }

    private void persistMovement(Map<String, String> values) {
        movementRepository.save(FinancialMovement.builder()
                .movementType(parseMovementType(values.get("movementType")))
                .amount(parseMoney(values.get("amount")))
                .description(blankToNull(values.get("description")))
                .category(blankToNull(values.get("category")))
                .movementDate(parseDate(values.get("movementDate")))
                .userId(currentUser.idOrNull())
                .build());
    }

    private void persistDebt(Map<String, String> values) {
        String document = values.get("document").trim();
        Customer customer = customerRepository.findByDocument(document).orElse(null);
        if (customer == null) {
            throw BusinessException.of("No hay cliente con documento %s", document);
        }
        BigDecimal original = parseMoney(values.get("originalAmount"));
        BigDecimal balance = values.get("balance") == null || values.get("balance").isBlank()
                ? original : parseMoney(values.get("balance"));
        LocalDate due = values.get("dueDate") == null || values.get("dueDate").isBlank()
                ? LocalDate.now() : parseDate(values.get("dueDate"));

        Long rentalId = null;
        if (values.get("customerName") != null && values.get("customerName").startsWith("ALQ-")) {
            rentalId = rentalRepository.findByCode(values.get("customerName").trim())
                    .map(rental -> rental.getId()).orElse(null);
        }

        debtRepository.save(Debt.builder()
                .customerId(customer.getId())
                .rentalId(rentalId)
                .originalAmount(original)
                .balance(balance)
                .dueDate(due)
                .status(Money.isZero(balance) ? DebtStatus.PAGADA : DebtStatus.ABIERTA)
                .build());
    }

    private List<String> validate(ImportJobType type, Map<String, String> values, boolean strict) {
        List<String> errors = new ArrayList<>();
        for (String field : ColumnAliases.requiredFields(type)) {
            if (values.get(field) == null || values.get(field).isBlank()) {
                errors.add("Falta el campo obligatorio '" + field + "'");
            }
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        try {
            switch (type) {
                case PRODUCTOS -> {
                    parseMoney(values.get("dailyPrice"));
                    parseInt(values.get("stockTotal"), 0);
                }
                case CLIENTES -> {
                    if (values.get("document").trim().length() < 4) {
                        errors.add("Documento demasiado corto");
                    }
                }
                case MOVIMIENTOS -> {
                    parseMoney(values.get("amount"));
                    parseMovementType(values.get("movementType"));
                    parseDate(values.get("movementDate"));
                }
                case DEUDAS -> parseMoney(values.get("originalAmount"));
            }
        } catch (BusinessException e) {
            errors.add(e.getMessage());
        }
        if (strict && type == ImportJobType.DEUDAS
                && customerRepository.findByDocument(values.get("document").trim()).isEmpty()) {
            errors.add("No existe un cliente con ese documento");
        }
        return errors;
    }

    private Map<String, String> applyMapping(Map<String, String> raw, Map<String, String> mapping) {
        Map<String, String> values = new LinkedHashMap<>();
        mapping.forEach((field, column) -> values.put(field, raw.getOrDefault(column, "")));
        return values;
    }

    private String storeErrorCsv(List<String> lines, int failed) {
        byte[] csv = String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
        String path = fileStorage.storeBytes(csv, FileStorageService.IMPORTS, "csv");
        return path + " | " + failed + " filas con error. Descarga el reporte para el detalle.";
    }

    private String errorPath(String errorLog) {
        if (errorLog == null || !errorLog.contains("|")) {
            return null;
        }
        String path = errorLog.split("\\|", 2)[0].trim();
        return fileStorage.exists(path) ? path : null;
    }

    private ImportJobView toView(ImportJob job) {
        return new ImportJobView(job.getId(), job.getJobType(), job.getFileName(), job.getFormat(),
                job.getStatus(), job.getRowsTotal(), job.getRowsOk(), job.getRowsError(),
                job.getErrorLog(), errorPath(job.getErrorLog()) != null,
                job.getStartedAt(), job.getFinishedAt());
    }

    private BigDecimal parseMoney(String raw) {
        if (raw == null || raw.isBlank()) {
            return Money.ZERO;
        }
        String cleaned = raw.replace("$", "").replace(" ", "").trim();
        if (cleaned.contains(",") && cleaned.contains(".")) {
            cleaned = cleaned.replace(".", "").replace(',', '.');
        } else if (cleaned.contains(",")) {
            cleaned = cleaned.replace(',', '.');
        } else if (cleaned.indexOf('.') != cleaned.lastIndexOf('.')) {
            cleaned = cleaned.replace(".", "");
        }
        try {
            return Money.of(new BigDecimal(cleaned));
        } catch (NumberFormatException e) {
            throw BusinessException.of("Valor numerico invalido: %s", raw);
        }
    }

    private BigDecimal parseMoneyOr(String raw, BigDecimal fallback) {
        return raw == null || raw.isBlank() ? Money.of(fallback) : parseMoney(raw);
    }

    private int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.replace(".", "").replace(",", "").trim());
        } catch (NumberFormatException e) {
            throw BusinessException.of("Entero invalido: %s", raw);
        }
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("La fecha es obligatoria");
        }
        String value = raw.trim();
        for (DateTimeFormatter formatter : DATES) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // siguiente formato
            }
        }
        throw BusinessException.of("Fecha invalida: %s", raw);
    }

    private MovementType parseMovementType(String raw) {
        if (raw == null) {
            throw new BusinessException("El tipo de movimiento es obligatorio");
        }
        String normalized = ColumnAliases.normalize(raw);
        if (normalized.contains("ingreso") || normalized.contains("entrada") || normalized.equals("i")) {
            return MovementType.INGRESO;
        }
        if (normalized.contains("gasto") || normalized.contains("egreso") || normalized.contains("salida")
                || normalized.equals("g")) {
            return MovementType.GASTO;
        }
        throw BusinessException.of("Tipo de movimiento desconocido: %s", raw);
    }

    private IdDocumentType parseDocumentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return IdDocumentType.CC;
        }
        String normalized = ColumnAliases.normalize(raw);
        return switch (normalized) {
            case "ce", "extranjeria" -> IdDocumentType.CE;
            case "nit" -> IdDocumentType.NIT;
            case "pasaporte", "passport" -> IdDocumentType.PASAPORTE;
            default -> IdDocumentType.CC;
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
