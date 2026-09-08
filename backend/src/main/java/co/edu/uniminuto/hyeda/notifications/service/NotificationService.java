package co.edu.uniminuto.hyeda.notifications.service;

import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.Kpis;
import co.edu.uniminuto.hyeda.analytics.service.AnalyticsService;
import co.edu.uniminuto.hyeda.analytics.service.ReportService;
import co.edu.uniminuto.hyeda.billing.domain.Debt;
import co.edu.uniminuto.hyeda.billing.repository.PaymentRepository;
import co.edu.uniminuto.hyeda.billing.service.PaymentService;
import co.edu.uniminuto.hyeda.crm.domain.Customer;
import co.edu.uniminuto.hyeda.crm.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.documents.domain.GeneratedDocument;
import co.edu.uniminuto.hyeda.documents.service.DocumentService;
import co.edu.uniminuto.hyeda.finance.service.DebtService;
import co.edu.uniminuto.hyeda.finance.service.FinanceService;
import co.edu.uniminuto.hyeda.notifications.domain.Notification;
import co.edu.uniminuto.hyeda.notifications.domain.NotificationStatus;
import co.edu.uniminuto.hyeda.notifications.domain.NotificationTemplate;
import co.edu.uniminuto.hyeda.notifications.repository.NotificationRepository;
import co.edu.uniminuto.hyeda.rental.domain.Rental;
import co.edu.uniminuto.hyeda.rental.repository.RentalRepository;
import co.edu.uniminuto.hyeda.shared.company.CompanySettings;
import co.edu.uniminuto.hyeda.shared.company.CompanySettingsService;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Disparadores de notificaciones del negocio.
 *
 * <p>Implementa el enganche de cierre del modulo de pagos para que, cuando un
 * alquiler queda pagado, salga sola la cuenta de cobro por WhatsApp. Todo lo
 * demas (resumen, recordatorios, reporte diario, cobro de cartera) lo dispara
 * el planificador.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements PaymentService.RentalClosureListener {

    /** Se avisa al cliente cuando le quedan estos dias o menos. */
    private static final long DUE_REMINDER_DAYS = 2;

    private final NotificationRepository repository;
    private final NotificationOutbox outbox;
    private final MessageComposer composer;
    private final CompanySettingsService companySettingsService;
    private final CustomerRepository customerRepository;
    private final RentalRepository rentalRepository;
    private final PaymentRepository paymentRepository;
    private final DebtService debtService;
    private final FinanceService financeService;
    private final DocumentService documentService;
    private final ReportService reportService;
    private final AnalyticsService analyticsService;
    private final FileStorageService fileStorage;

    // ------------------------------------------------------ disparadores

    /** Resumen del alquiler al cliente, apenas se abre. */
    @Transactional
    public void sendRentalSummary(Long rentalId) {
        Rental rental = rental(rentalId);
        Customer customer = customer(rental.getCustomerId());

        BigDecimal paid = Money.of(paymentRepository.netPaidForRental(rentalId));
        BigDecimal pending = Money.atLeastZero(Money.subtract(rental.currentAmount(), paid));

        outbox.enqueue(NotificationTemplate.RESUMEN_ALQUILER, customer.getPhone(),
                composer.rentalSummary(company(), customer, rental, paid, pending),
                rentalId, customer.getId(), null);
    }

    /** Cuenta de cobro automatica al cerrar el alquiler con pago completo. */
    @Override
    @Transactional
    public void onRentalClosed(Rental rental) {
        Customer customer = customer(rental.getCustomerId());
        GeneratedDocument invoice = documentService.issueInvoice(rental.getId());

        outbox.enqueue(NotificationTemplate.FACTURA_CIERRE, customer.getPhone(),
                composer.invoiceCaption(company(), customer, rental),
                rental.getId(), customer.getId(), invoice.getFilePath());
    }

    /** Recordatorio a los clientes cuyo plazo esta por vencerse. */
    @Transactional
    public int sendDueReminders() {
        LocalDate limit = LocalDate.now().plusDays(DUE_REMINDER_DAYS);
        int queued = 0;

        for (Rental rental : rentalRepository.findActive()) {
            if (rental.getEndDate().isAfter(limit)) {
                continue;
            }
            boolean alreadySent = repository.existsByRentalIdAndTemplateCodeAndStatusIn(
                    rental.getId(), NotificationTemplate.RECORDATORIO_VENCIMIENTO,
                    List.of(NotificationStatus.EN_COLA, NotificationStatus.ENVIADA));
            if (alreadySent) {
                continue;
            }

            Customer customer = customerRepository.findById(rental.getCustomerId()).orElse(null);
            if (customer == null) {
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), rental.getEndDate());
            outbox.enqueue(NotificationTemplate.RECORDATORIO_VENCIMIENTO, customer.getPhone(),
                    composer.dueReminder(company(), customer, rental, daysLeft),
                    rental.getId(), customer.getId(), null);
            queued++;
        }
        return queued;
    }

    /**
     * Reporte diario en PDF a la administradora, con todo lo que paso en el dia
     * y que material sigue afuera.
     */
    @Transactional
    public Notification sendDailyReport(LocalDate date) {
        CompanySettings company = company();
        requireAdminNumber(company);

        byte[] pdf = reportService.dailyReportPdf(date);
        GeneratedDocument stored = documentService.storeReport(pdf,
                "Reporte diario " + date);

        Kpis kpis = analyticsService.dashboard(date, date).kpis();
        String caption = composer.dailyReportCaption(company, date, kpis.rentalsOpened(),
                financeService.incomeBetween(date, date), financeService.expenseBetween(date, date),
                kpis.openDebt(), kpis.unitsOut());

        return outbox.enqueue(NotificationTemplate.REPORTE_DIARIO, company.getWhatsappAdmin(),
                caption, null, null, stored.getFilePath());
    }

    /**
     * Seguimiento de cartera: cobra al cliente moroso y le manda a la
     * administradora el consolidado para que sepa a quien llamar.
     */
    @Transactional
    public int sendOverdueFollowUp(int minDays) {
        CompanySettings company = company();
        List<Debt> overdue = debtService.overdueBeyond(minDays);

        List<String> digest = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int queued = 0;

        for (Debt debt : overdue) {
            Customer customer = customerRepository.findById(debt.getCustomerId()).orElse(null);
            if (customer == null) {
                continue;
            }
            String rentalCode = debt.getRentalId() == null ? null
                    : rentalRepository.findById(debt.getRentalId()).map(Rental::getCode).orElse(null);

            digest.add("%s (%s): %s, %d dias".formatted(customer.getName(), customer.getDocument(),
                    Money.format(debt.getBalance()), debt.getOverdueDays()));
            total = total.add(debt.getBalance());

            // Al cliente se le escribe como maximo una vez por semana.
            boolean recentlyContacted = repository.existsByCustomerIdAndTemplateCodeAndCreatedAtAfter(
                    customer.getId(), NotificationTemplate.COBRO_DEUDA,
                    LocalDateTime.now().minusDays(7));
            if (!recentlyContacted) {
                outbox.enqueue(NotificationTemplate.COBRO_DEUDA, customer.getPhone(),
                        composer.debtReminder(company, customer, debt, rentalCode),
                        debt.getRentalId(), customer.getId(), null);
                debtService.markNoticeSent(debt.getId());
                queued++;
            }
        }

        if (company.getWhatsappAdmin() != null) {
            outbox.enqueue(NotificationTemplate.SEGUIMIENTO_MOROSOS, company.getWhatsappAdmin(),
                    composer.overdueDigest(company, digest, Money.of(total)), null, null, null);
        }
        return queued;
    }

    /** Envio manual desde la pantalla de notificaciones. */
    @Transactional
    public Notification sendManual(String recipient, String message, Long customerId, Long rentalId) {
        return outbox.enqueue(NotificationTemplate.MANUAL, recipient, message,
                rentalId, customerId, null);
    }

    /** Reenvia un documento ya generado al cliente del alquiler. */
    @Transactional
    public Notification resendDocument(Long documentId, String message) {
        GeneratedDocument document = documentService.find(documentId);
        if (document.getCustomerId() == null) {
            throw new BusinessException("Este documento no esta asociado a un cliente");
        }
        Customer customer = customer(document.getCustomerId());
        if (!fileStorage.exists(document.getFilePath())) {
            throw new BusinessException("El archivo del documento ya no esta disponible");
        }

        return outbox.enqueue(NotificationTemplate.MANUAL, customer.getPhone(),
                message == null || message.isBlank()
                        ? "Adjuntamos el documento %s de %s.".formatted(
                                document.getSerialNumber(), company().getTradeName())
                        : message,
                document.getRentalId(), customer.getId(), document.getFilePath());
    }

    // --------------------------------------------------------------- bandeja

    @Transactional(readOnly = true)
    public PageResponse<Notification> inbox(NotificationStatus status, NotificationTemplate template,
                                            Pageable pageable) {
        return PageResponse.from(repository.search(status, template, pageable));
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return repository.countByStatus(NotificationStatus.EN_COLA);
    }

    @Transactional
    public Notification retry(Long id) {
        return outbox.retry(id);
    }

    // -------------------------------------------------------------- internos

    private CompanySettings company() {
        return companySettingsService.get();
    }

    private void requireAdminNumber(CompanySettings company) {
        if (!PhoneNumbers.isUsable(company.getWhatsappAdmin())) {
            throw new BusinessException(
                    "Configura el WhatsApp de la administradora para poder enviarle el reporte");
        }
    }

    private Rental rental(Long rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() -> ResourceNotFoundException.of("Alquiler", rentalId));
    }

    private Customer customer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Cliente", customerId));
    }
}
