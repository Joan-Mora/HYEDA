package co.edu.uniminuto.hyeda.documents.service;

import co.edu.uniminuto.hyeda.billing.domain.Payment;
import co.edu.uniminuto.hyeda.billing.repository.PaymentRepository;
import co.edu.uniminuto.hyeda.crm.domain.Customer;
import co.edu.uniminuto.hyeda.crm.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.documents.domain.GeneratedDocument;
import co.edu.uniminuto.hyeda.documents.domain.GeneratedDocumentType;
import co.edu.uniminuto.hyeda.documents.dto.DocumentDtos.DocumentRow;
import co.edu.uniminuto.hyeda.documents.dto.DocumentDtos.DocumentVerification;
import co.edu.uniminuto.hyeda.documents.repository.GeneratedDocumentRepository;
import co.edu.uniminuto.hyeda.rental.domain.DocumentPolicy;
import co.edu.uniminuto.hyeda.rental.domain.Rental;
import co.edu.uniminuto.hyeda.rental.domain.RentalItem;
import co.edu.uniminuto.hyeda.rental.domain.RentalStatus;
import co.edu.uniminuto.hyeda.rental.domain.ReturnIncident;
import co.edu.uniminuto.hyeda.rental.repository.RentalRepository;
import co.edu.uniminuto.hyeda.rental.repository.ReturnIncidentRepository;
import co.edu.uniminuto.hyeda.rental.service.RentalService;
import co.edu.uniminuto.hyeda.shared.audit.AuditService;
import co.edu.uniminuto.hyeda.shared.company.CompanySettings;
import co.edu.uniminuto.hyeda.shared.company.CompanySettingsService;
import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import co.edu.uniminuto.hyeda.shared.domain.SpanishNumbers;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.security.CurrentUser;
import co.edu.uniminuto.hyeda.shared.storage.FileStorageService;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emision y consulta de los documentos legales del negocio.
 *
 * <p>Un alquiler se respalda con contrato o con remision, nunca con ambos: el
 * contrato de arrendamiento ya relaciona los equipos entregados. La devolucion
 * cierra la remision, y la cuenta de cobro se emite con la liquidacion final.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final GeneratedDocumentRepository documentRepository;
    private final RentalRepository rentalRepository;
    private final ReturnIncidentRepository incidentRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final CompanySettingsService companySettingsService;
    private final DocumentNumberService numberService;
    private final PdfRenderer pdfRenderer;
    private final QrCodeService qrCodeService;
    private final FileStorageService fileStorage;
    private final RentalService rentalService;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final HyedaProperties properties;

    // ------------------------------------------------------------- consultas

    @Transactional(readOnly = true)
    public PageResponse<DocumentRow> search(String search, GeneratedDocumentType type,
                                            LocalDate from, LocalDate to, Pageable pageable) {
        return PageResponse.from(documentRepository.search(
                blankToNull(search), type,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.atTime(23, 59, 59),
                pageable), this::toRow);
    }

    @Transactional(readOnly = true)
    public List<DocumentRow> byRental(Long rentalId) {
        return documentRepository.findByRentalIdOrderByGeneratedAtDesc(rentalId).stream()
                .map(this::toRow).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentRow> byCustomer(Long customerId) {
        return documentRepository.findByCustomerIdOrderByGeneratedAtDesc(customerId).stream()
                .map(this::toRow).toList();
    }

    /** Bytes del PDF, para la vista previa embebida y para la descarga. */
    @Transactional(readOnly = true)
    public byte[] content(Long id) {
        return fileStorage.read(find(id).getFilePath());
    }

    @Transactional(readOnly = true)
    public GeneratedDocument find(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Documento", id));
    }

    /**
     * Verificacion publica del QR impreso. No expone el documento completo: solo
     * confirma que el consecutivo existe y a nombre de quien se emitio.
     */
    @Transactional(readOnly = true)
    public DocumentVerification verify(String serialNumber) {
        return documentRepository.findBySerialNumber(serialNumber)
                .map(document -> {
                    String issuedTo = document.getCustomerId() == null ? null
                            : customerRepository.findById(document.getCustomerId())
                                    .map(Customer::getName).orElse(null);
                    return new DocumentVerification(true, document.getSerialNumber(),
                            document.getDocumentType().title(), issuedTo, document.getGeneratedAt(),
                            "Documento emitido por " + companySettingsService.get().getLegalName());
                })
                .orElseGet(() -> new DocumentVerification(false, serialNumber, null, null, null,
                        "No existe ningun documento con este consecutivo"));
    }

    // -------------------------------------------------------------- emision

    /**
     * Emite el documento de salida que corresponde segun la politica del
     * alquiler y deja el alquiler listo para entregar el material.
     */
    @Transactional
    public GeneratedDocument issueRentalDocument(Long rentalId) {
        Rental rental = rental(rentalId);
        GeneratedDocumentType type = rental.getDocumentPolicy() == DocumentPolicy.CONTRATO
                ? GeneratedDocumentType.CONTRATO
                : GeneratedDocumentType.REMISION;

        GeneratedDocument existing = documentRepository
                .findFirstByRentalIdAndDocumentTypeOrderByGeneratedAtDesc(rentalId, type)
                .orElse(null);
        if (existing != null) {
            // Reemitir no debe consumir otro consecutivo ni duplicar el legajo.
            return existing;
        }

        GeneratedDocument document = type == GeneratedDocumentType.CONTRATO
                ? renderContract(rental)
                : renderDeliveryNote(rental);

        rentalService.markDocumentIssued(rentalId, "%s %s emitido".formatted(
                type.title(), document.getSerialNumber()));

        auditService.record("EMITIR_DOCUMENTO", "Rental", rentalId,
                "%s %s".formatted(type.title(), document.getSerialNumber()));
        return document;
    }

    /** Acta de devolucion; solo tiene sentido cuando el material ya regreso. */
    @Transactional
    public GeneratedDocument issueReturnReceipt(Long rentalId) {
        Rental rental = rental(rentalId);
        if (rental.getActualReturnDate() == null) {
            throw new BusinessException("Todavia no se ha registrado la devolucion de este alquiler");
        }
        return documentRepository
                .findFirstByRentalIdAndDocumentTypeOrderByGeneratedAtDesc(
                        rentalId, GeneratedDocumentType.DEVOLUCION)
                .orElseGet(() -> renderReturnReceipt(rental));
    }

    /**
     * Cuenta de cobro con la liquidacion definitiva. Se regenera cada vez que se
     * pide, porque el saldo cambia con cada abono; el consecutivo se conserva.
     */
    @Transactional
    public GeneratedDocument issueInvoice(Long rentalId) {
        Rental rental = rental(rentalId);
        if (rental.getFinalAmount() == null) {
            throw new BusinessException(
                    "La cuenta de cobro se emite despues de liquidar la devolucion del material");
        }

        GeneratedDocument previous = documentRepository
                .findFirstByRentalIdAndDocumentTypeOrderByGeneratedAtDesc(
                        rentalId, GeneratedDocumentType.FACTURA)
                .orElse(null);
        String serial = previous != null
                ? previous.getSerialNumber()
                : numberService.next(GeneratedDocumentType.FACTURA);

        Customer customer = customer(rental);
        Map<String, Object> model = baseModel(rental, customer, GeneratedDocumentType.FACTURA, serial);
        fillSettlement(model, rental);
        model.put("periodLabel", "%s al %s".formatted(
                rental.getStartDate().format(DATE),
                (rental.getActualReturnDate() == null ? rental.getEndDate() : rental.getActualReturnDate())
                        .format(DATE)));
        model.put("payments", paymentRows(rentalId));

        byte[] pdf = pdfRenderer.render(GeneratedDocumentType.FACTURA.templatePath(), model);
        String path = fileStorage.storeBytes(pdf, FileStorageService.DOCUMENTS, "pdf");

        if (previous != null) {
            // Misma cuenta de cobro, contenido actualizado con los pagos recientes.
            previous.setFilePath(path);
            previous.setGeneratedAt(LocalDateTime.now());
            return documentRepository.save(previous);
        }

        GeneratedDocument document = persist(rental, customer, GeneratedDocumentType.FACTURA, serial, path,
                "Cuenta de cobro %s - %s".formatted(serial, rental.getCode()));
        auditService.record("EMITIR_FACTURA", "Rental", rentalId, serial);
        return document;
    }

    /**
     * Guarda un reporte ya renderizado (diario, analitico) en el legajo, para
     * que quede disponible en la seccion de documentos y en WhatsApp.
     */
    @Transactional
    public GeneratedDocument storeReport(byte[] pdf, String title) {
        String serial = numberService.next(GeneratedDocumentType.REPORTE);
        String path = fileStorage.storeBytes(pdf, FileStorageService.REPORTS, "pdf");

        return documentRepository.save(GeneratedDocument.builder()
                .documentType(GeneratedDocumentType.REPORTE)
                .serialNumber(serial)
                .filePath(path)
                .title(title)
                .generatedBy(currentUser.idOrNull())
                .build());
    }

    /** Consecutivo de reporte sin persistir el archivo; lo usa el generador de reportes. */
    public String nextReportSerial() {
        return numberService.next(GeneratedDocumentType.REPORTE);
    }

    /** Modelo comun (empresa, QR, fechas) para las plantillas que no cuelgan de un alquiler. */
    public Map<String, Object> reportModel(String serial, String reportKind) {
        CompanySettings company = companySettingsService.get();
        Map<String, Object> model = new HashMap<>();
        model.put("company", company);
        model.put("serial", serial);
        model.put("reportKind", reportKind);
        model.put("issuedDate", LocalDate.now().format(DATE));
        model.put("generatedAt", LocalDateTime.now().format(DATE_TIME));
        model.put("verifyUrl", verifyUrl(serial));
        model.put("qrDataUri", qrCodeService.dataUri(verifyUrl(serial), 200));
        return model;
    }

    public byte[] render(String templatePath, Map<String, Object> model) {
        return pdfRenderer.render(templatePath, model);
    }

    /** Marca el documento como firmado cuando el cliente estampa su firma. */
    @Transactional
    public GeneratedDocument markSigned(Long id) {
        GeneratedDocument document = find(id);
        document.setSigned(true);
        document.setSignedAt(LocalDateTime.now());
        auditService.record("FIRMAR_DOCUMENTO", "GeneratedDocument", id, document.getSerialNumber());
        return documentRepository.save(document);
    }

    // ------------------------------------------------------------ renderizado

    private GeneratedDocument renderContract(Rental rental) {
        Customer customer = customer(rental);
        String serial = numberService.next(GeneratedDocumentType.CONTRATO);

        Map<String, Object> model = baseModel(rental, customer, GeneratedDocumentType.CONTRATO, serial);
        model.put("subtotalLabel", Money.format(rental.getItemsSubtotal()));
        model.put("totalLabel", Money.format(rental.getEstimatedTotal()));
        model.put("totalInWords", SpanishNumbers.pesos(rental.getEstimatedTotal()));
        model.put("consentDateLabel", customer.getDataConsentAt() == null
                ? null : customer.getDataConsentAt().format(DATE));

        byte[] pdf = pdfRenderer.render(GeneratedDocumentType.CONTRATO.templatePath(), model);
        String path = fileStorage.storeBytes(pdf, FileStorageService.DOCUMENTS, "pdf");

        return persist(rental, customer, GeneratedDocumentType.CONTRATO, serial, path,
                "Contrato %s - %s".formatted(serial, customer.getName()));
    }

    private GeneratedDocument renderDeliveryNote(Rental rental) {
        Customer customer = customer(rental);
        String serial = numberService.next(GeneratedDocumentType.REMISION);

        Map<String, Object> model = baseModel(rental, customer, GeneratedDocumentType.REMISION, serial);
        model.put("subtotalLabel", Money.format(rental.getItemsSubtotal()));
        model.put("totalLabel", Money.format(rental.getEstimatedTotal()));
        model.put("totalInWords", SpanishNumbers.pesos(rental.getEstimatedTotal()));

        byte[] pdf = pdfRenderer.render(GeneratedDocumentType.REMISION.templatePath(), model);
        String path = fileStorage.storeBytes(pdf, FileStorageService.DOCUMENTS, "pdf");

        return persist(rental, customer, GeneratedDocumentType.REMISION, serial, path,
                "Remision %s - %s".formatted(serial, customer.getName()));
    }

    private GeneratedDocument renderReturnReceipt(Rental rental) {
        Customer customer = customer(rental);
        String serial = numberService.next(GeneratedDocumentType.DEVOLUCION);

        Map<String, Object> model = baseModel(rental, customer, GeneratedDocumentType.DEVOLUCION, serial);
        fillSettlement(model, rental);
        model.put("relatedSerial", documentRepository
                .findFirstByRentalIdAndDocumentTypeOrderByGeneratedAtDesc(
                        rental.getId(), GeneratedDocumentType.REMISION)
                .map(GeneratedDocument::getSerialNumber)
                .orElseGet(() -> documentRepository
                        .findFirstByRentalIdAndDocumentTypeOrderByGeneratedAtDesc(
                                rental.getId(), GeneratedDocumentType.CONTRATO)
                        .map(GeneratedDocument::getSerialNumber)
                        .orElse(null)));

        byte[] pdf = pdfRenderer.render(GeneratedDocumentType.DEVOLUCION.templatePath(), model);
        String path = fileStorage.storeBytes(pdf, FileStorageService.DOCUMENTS, "pdf");

        GeneratedDocument document = persist(rental, customer, GeneratedDocumentType.DEVOLUCION, serial, path,
                "Devolucion %s - %s".formatted(serial, customer.getName()));
        auditService.record("EMITIR_DEVOLUCION", "Rental", rental.getId(), serial);
        return document;
    }

    // --------------------------------------------------------------- modelo

    private Map<String, Object> baseModel(Rental rental, Customer customer,
                                          GeneratedDocumentType type, String serial) {
        Map<String, Object> model = new HashMap<>();
        model.put("company", companySettingsService.get());
        model.put("customer", customer);
        model.put("rental", rental);
        model.put("serial", serial);
        model.put("issuedDate", LocalDate.now().format(DATE));
        model.put("generatedAt", LocalDateTime.now().format(DATE_TIME));
        model.put("verifyUrl", verifyUrl(serial));
        model.put("qrDataUri", qrCodeService.dataUri(verifyUrl(serial), 220));
        model.put("signatureDataUri", signatureDataUri(rental));

        model.put("startDateLabel", rental.getStartDate().format(DATE));
        model.put("endDateLabel", rental.getEndDate().format(DATE));
        model.put("returnDateLabel", rental.getActualReturnDate() == null
                ? null : rental.getActualReturnDate().format(DATE));

        model.put("haulingLabel", Money.format(rental.getHaulingCost()));
        model.put("discountLabel", Money.format(rental.getDiscountAmount()));
        model.put("hasDiscount", Money.isPositive(rental.getDiscountAmount()));
        model.put("documentKind", type.title());
        model.put("items", itemRows(rental));
        return model;
    }

    /** Cifras de cierre compartidas por el acta de devolucion y la cuenta de cobro. */
    private void fillSettlement(Map<String, Object> model, Rental rental) {
        BigDecimal total = rental.currentAmount();
        BigDecimal paid = Money.of(paymentRepository.netPaidForRental(rental.getId()));
        BigDecimal difference = Money.subtract(total, paid);
        boolean hasRefund = difference.signum() < 0;

        int graceDays = companySettingsService.get().getLateGraceDays();
        int actualDays = rental.getActualDays() == null ? rental.getEstimatedDays() : rental.getActualDays();
        int chargedDays = Math.min(actualDays, rental.getEstimatedDays());
        int lateDays = Math.max(0, actualDays - rental.getEstimatedDays() - graceDays);

        model.put("subtotalLabel", Money.format(rental.getItemsSubtotal()));
        model.put("totalLabel", Money.format(total));
        model.put("totalInWords", SpanishNumbers.pesos(total));
        model.put("paidLabel", Money.format(paid));
        model.put("balanceLabel", Money.format(difference.abs()));
        model.put("hasRefund", hasRefund);
        model.put("hasBalance", difference.signum() > 0);

        model.put("chargedDays", chargedDays);
        model.put("chargedDaysLabel", chargedDays + (chargedDays == 1 ? " dia" : " dias"));
        model.put("lateDaysLabel", lateDays + (lateDays == 1 ? " dia" : " dias"));
        model.put("hasLateFee", Money.isPositive(rental.getLateFeeTotal()));
        model.put("lateFeeLabel", Money.format(rental.getLateFeeTotal()));
        model.put("hasIncidentCharges", Money.isPositive(rental.getIncidentChargeTotal()));
        model.put("incidentTotalLabel", Money.format(rental.getIncidentChargeTotal()));
        model.put("earlyReturn", actualDays < rental.getEstimatedDays());

        List<ReturnIncident> incidents =
                incidentRepository.findByRentalIdOrderByReportedAtAsc(rental.getId());
        model.put("incidents", incidentRows(rental, incidents));
        model.put("items", itemRows(rental, incidents, chargedDays));
    }

    private List<Map<String, Object>> itemRows(Rental rental) {
        return itemRows(rental, List.of(), rental.getEstimatedDays());
    }

    private List<Map<String, Object>> itemRows(Rental rental, List<ReturnIncident> incidents, int billedDays) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (RentalItem item : rental.getItems()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productName", item.getProductName());
            row.put("quantity", item.getQuantity());
            row.put("quantityInWords", SpanishNumbers.capitalized(item.getQuantity()));
            row.put("quantityReturned", item.getQuantityReturned());
            row.put("returnedInWords", SpanishNumbers.capitalized(item.getQuantityReturned()));
            row.put("unitPriceLabel", Money.format(item.getUnitPrice()));
            row.put("lateFeeLabel", Money.format(Money.isZero(item.getLateFeeSnapshot())
                    ? item.getUnitPrice() : item.getLateFeeSnapshot()));
            row.put("billedDays", billedDays);
            row.put("lineLabel", Money.format(item.getLineFinal() != null
                    ? item.getLineFinal() : item.getLineEstimated()));
            row.put("conditionLabel", conditionLabel(item, incidents));
            rows.add(row);
        }
        return rows;
    }

    private String conditionLabel(RentalItem item, List<ReturnIncident> incidents) {
        String reported = incidents.stream()
                .filter(incident -> item.getId() != null && item.getId().equals(incident.getRentalItemId()))
                .map(incident -> incident.getItemCondition().name())
                .distinct()
                .reduce((left, right) -> left + " / " + right)
                .orElse(null);
        if (reported != null) {
            return reported;
        }
        return item.pendingReturn() == 0 ? "Conforme" : "Pendiente";
    }

    private List<Map<String, Object>> incidentRows(Rental rental, List<ReturnIncident> incidents) {
        Map<Long, String> namesByItem = new HashMap<>();
        rental.getItems().forEach(item -> namesByItem.put(item.getId(), item.getProductName()));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ReturnIncident incident : incidents) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("condition", incident.getItemCondition().name());
            row.put("productName", namesByItem.get(incident.getRentalItemId()));
            row.put("quantity", incident.getQuantity());
            row.put("reason", incident.getReason());
            row.put("chargeLabel", Money.format(incident.getChargeAmount()));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> paymentRows(Long rentalId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Payment payment : paymentRepository.findByRentalIdOrderByCreatedAtAsc(rentalId)) {
            if (!payment.isConfirmed()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            LocalDateTime moment = payment.getPaidAt() == null ? payment.getCreatedAt() : payment.getPaidAt();
            row.put("dateLabel", moment.format(DATE));
            row.put("type", payment.getPaymentType().name());
            row.put("reference", payment.getReference());
            row.put("amountLabel", Money.format(payment.getAmount()));
            rows.add(row);
        }
        return rows;
    }

    // -------------------------------------------------------------- internos

    private GeneratedDocument persist(Rental rental, Customer customer, GeneratedDocumentType type,
                                      String serial, String path, String title) {
        return documentRepository.save(GeneratedDocument.builder()
                .rentalId(rental.getId())
                .customerId(customer.getId())
                .documentType(type)
                .serialNumber(serial)
                .filePath(path)
                .title(title)
                .signed(rental.getSignaturePath() != null)
                .signedAt(rental.getSignaturePath() == null ? null : LocalDateTime.now())
                .generatedBy(currentUser.idOrNull())
                .build());
    }

    private Rental rental(Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> ResourceNotFoundException.of("Alquiler", rentalId));
        if (rental.getStatus() == RentalStatus.CANCELADO) {
            throw new BusinessException("No se emiten documentos de un alquiler cancelado");
        }
        return rental;
    }

    private Customer customer(Rental rental) {
        return customerRepository.findById(rental.getCustomerId())
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", rental.getCustomerId()));
    }

    private String verifyUrl(String serial) {
        return "%s/verificar/%s".formatted(trimTrailingSlash(properties.getPublicBaseUrl()), serial);
    }

    /** La firma se guarda como PNG en disco y se incrusta en el PDF como data URI. */
    private String signatureDataUri(Rental rental) {
        if (rental.getSignaturePath() == null || !fileStorage.exists(rental.getSignaturePath())) {
            return null;
        }
        try {
            return "data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(fileStorage.read(rental.getSignaturePath()));
        } catch (RuntimeException e) {
            log.warn("No se pudo incrustar la firma del alquiler {}", rental.getCode(), e);
            return null;
        }
    }

    @Transactional(readOnly = true)
    public DocumentRow toRow(GeneratedDocument document) {
        Customer customer = document.getCustomerId() == null ? null
                : customerRepository.findById(document.getCustomerId()).orElse(null);
        String rentalCode = document.getRentalId() == null ? null
                : rentalRepository.findById(document.getRentalId()).map(Rental::getCode).orElse(null);

        return new DocumentRow(document.getId(), document.getDocumentType(),
                document.getDocumentType().title(), document.getSerialNumber(), document.getTitle(),
                document.getRentalId(), rentalCode, document.getCustomerId(),
                customer == null ? null : customer.getName(),
                customer == null ? null : customer.getDocument(),
                Boolean.TRUE.equals(document.getSigned()), document.getGeneratedAt(),
                "/api/documentos/%d/contenido".formatted(document.getId()),
                "/api/documentos/%d/descargar".formatted(document.getId()));
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
