package co.edu.uniminuto.hyeda.billing.service;

import co.edu.uniminuto.hyeda.billing.domain.Payment;
import co.edu.uniminuto.hyeda.billing.domain.PaymentStatus;
import co.edu.uniminuto.hyeda.billing.domain.PaymentType;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.CashPaymentRequest;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.ConfirmPaymentRequest;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.CreditApplicationRequest;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.PaymentView;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.QrPaymentRequest;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.RentalAccount;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.SettlementRequest;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.SurplusHandling;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.TransferPaymentRequest;
import co.edu.uniminuto.hyeda.billing.repository.PaymentRepository;
import co.edu.uniminuto.hyeda.crm.domain.Customer;
import co.edu.uniminuto.hyeda.crm.repository.CustomerRepository;
import co.edu.uniminuto.hyeda.documents.service.QrCodeService;
import co.edu.uniminuto.hyeda.finance.domain.MovementType;
import co.edu.uniminuto.hyeda.finance.service.DebtService;
import co.edu.uniminuto.hyeda.finance.service.FinanceService;
import co.edu.uniminuto.hyeda.integrations.port.PaymentGatewayPort;
import co.edu.uniminuto.hyeda.rental.domain.Rental;
import co.edu.uniminuto.hyeda.rental.domain.RentalStatus;
import co.edu.uniminuto.hyeda.rental.repository.RentalRepository;
import co.edu.uniminuto.hyeda.rental.service.RentalService;
import co.edu.uniminuto.hyeda.shared.audit.AuditService;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Cobro y cierre economico del alquiler.
 *
 * <p>El alquiler que se abre en el mostrador es una estimacion. El cliente puede
 * pagar todo, abonar o irse sin pagar; en los tres casos el flujo continua. La
 * cifra definitiva se calcula cuando devuelve el material, y es ahi cuando se
 * decide si queda deuda, si hay que devolverle plata o si se cierra en firme.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    /** Dias que se le dan al cliente para pagar el saldo tras la liquidacion. */
    private static final int DEBT_GRACE_DAYS = 8;

    private final PaymentRepository paymentRepository;
    private final RentalRepository rentalRepository;
    private final CustomerRepository customerRepository;
    private final RentalService rentalService;
    private final DebtService debtService;
    private final FinanceService financeService;
    private final QrCodeService qrCodeService;
    private final PaymentGatewayPort paymentGateway;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    private final java.util.Optional<RentalClosureListener> closureListener;

    /**
     * Enganche para lo que debe ocurrir cuando un alquiler queda cerrado y
     * pagado: emitir la cuenta de cobro y enviarla por WhatsApp. Se declara como
     * puerto para que el modulo de pagos no dependa de las notificaciones.
     */
    public interface RentalClosureListener {
        void onRentalClosed(Rental rental);
    }

    // -------------------------------------------------------------- consultas

    @Transactional(readOnly = true)
    public RentalAccount account(Long rentalId) {
        return toAccount(rental(rentalId));
    }

    @Transactional(readOnly = true)
    public List<PaymentView> paymentsOf(Long rentalId) {
        return paymentRepository.findByRentalIdOrderByCreatedAtAsc(rentalId).stream()
                .map(payment -> toView(payment, null))
                .toList();
    }

    // ------------------------------------------------------------------ cobro

    /** Efectivo en el mostrador. Calcula el vuelto sobre lo que entrega el cliente. */
    @Transactional
    public PaymentView payCash(Long rentalId, CashPaymentRequest request) {
        Rental rental = rental(rentalId);
        BigDecimal amount = Money.of(request.amount());
        requireWithinBalance(rental, amount);

        BigDecimal tendered = request.amountTendered() == null ? amount : Money.of(request.amountTendered());
        if (Money.gt(amount, tendered)) {
            throw BusinessException.of("El cliente entrego %s, no alcanza para cubrir %s",
                    Money.format(tendered), Money.format(amount));
        }

        Payment payment = Payment.builder()
                .rentalId(rentalId)
                .paymentType(PaymentType.EFECTIVO)
                .amount(amount)
                .amountTendered(tendered)
                .changeGiven(Money.subtract(tendered, amount))
                .status(PaymentStatus.CONFIRMADO)
                .paidAt(LocalDateTime.now())
                .note(request.note())
                .recordedBy(currentUser.idOrNull())
                .build();

        return toView(confirmAndPost(rental, payment), null);
    }

    @Transactional
    public PaymentView payTransfer(Long rentalId, TransferPaymentRequest request) {
        Rental rental = rental(rentalId);
        BigDecimal amount = Money.of(request.amount());
        requireWithinBalance(rental, amount);

        Payment payment = Payment.builder()
                .rentalId(rentalId)
                .paymentType(PaymentType.TRANSFERENCIA)
                .amount(amount)
                .status(PaymentStatus.CONFIRMADO)
                .reference(blankToNull(request.reference()))
                .paidAt(LocalDateTime.now())
                .note(request.note())
                .recordedBy(currentUser.idOrNull())
                .build();

        return toView(confirmAndPost(rental, payment), null);
    }

    /**
     * Emite el QR para que el cliente pague escaneando. El pago queda pendiente
     * hasta que el operador confirma que el dinero llego.
     */
    @Transactional
    public PaymentView createQrCharge(Long rentalId, QrPaymentRequest request) {
        Rental rental = rental(rentalId);
        BigDecimal amount = Money.of(request.amount());
        requireWithinBalance(rental, amount);

        String reference = "HYEDA-%s-%s".formatted(rental.getCode(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        PaymentGatewayPort.ChargeIntent intent = paymentGateway.createCharge(reference, amount,
                "Alquiler " + rental.getCode());

        Payment payment = paymentRepository.save(Payment.builder()
                .rentalId(rentalId)
                .paymentType(PaymentType.QR)
                .amount(amount)
                .status(PaymentStatus.PENDIENTE)
                .reference(reference)
                .provider(paymentGateway.provider())
                .providerPayload(intent.payload())
                .qrContent(intent.qrContent())
                .note(request.note())
                .recordedBy(currentUser.idOrNull())
                .build());

        auditService.record("EMITIR_QR_PAGO", "Payment", payment.getId(),
                "%s por %s".formatted(rental.getCode(), Money.format(amount)));

        return toView(payment, intent.checkoutUrl());
    }

    /** Confirma un cobro por QR o transferencia que estaba a la espera. */
    @Transactional
    public PaymentView confirm(Long paymentId, ConfirmPaymentRequest request) {
        Payment payment = payment(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDIENTE) {
            throw BusinessException.of("El pago ya esta en estado %s", payment.getStatus());
        }

        Rental rental = rental(payment.getRentalId());
        payment.setStatus(PaymentStatus.CONFIRMADO);
        payment.setPaidAt(LocalDateTime.now());
        if (request != null && request.reference() != null && !request.reference().isBlank()) {
            payment.setReference(request.reference().trim());
        }
        if (request != null && request.note() != null && !request.note().isBlank()) {
            payment.setNote(request.note().trim());
        }

        return toView(confirmAndPost(rental, payment), null);
    }

    @Transactional
    public PaymentView cancel(Long paymentId, String reason) {
        Payment payment = payment(paymentId);
        if (payment.getStatus() == PaymentStatus.ANULADO) {
            return toView(payment, null);
        }

        boolean wasConfirmed = payment.isConfirmed();
        payment.setStatus(PaymentStatus.ANULADO);
        payment.setNote(appendNote(payment.getNote(), reason));
        paymentRepository.save(payment);

        if (wasConfirmed) {
            // Sacarlo de caja tambien: si no, el libro queda cuadrado con plata que no entro.
            financeService.reversePayment(paymentId);
            recomputeDebt(rental(payment.getRentalId()));
        }

        auditService.record("ANULAR_PAGO", "Payment", paymentId, reason);
        return toView(payment, null);
    }

    /**
     * El cliente se lleva el material sin pagar. No es un error: es un caso
     * corriente en la empresa, pero queda la marca y el saldo pendiente.
     */
    @Transactional
    public RentalAccount continueWithoutPayment(Long rentalId, String note) {
        Rental rental = rental(rentalId);
        BigDecimal balance = balanceOf(rental);
        if (!Money.isPositive(balance)) {
            throw new BusinessException("Este alquiler ya esta pagado; no hay nada que dejar pendiente");
        }

        rental.setNotes(appendNote(rental.getNotes(),
                "SIN PAGO al momento de la entrega: queda pendiente %s. %s"
                        .formatted(Money.format(balance), note == null ? "" : note).trim()));
        rentalRepository.save(rental);

        auditService.record("CONTINUAR_SIN_PAGO", "Rental", rentalId, Money.format(balance));
        return toAccount(rental);
    }

    /** Usa el saldo a favor del cliente como forma de pago de este alquiler. */
    @Transactional
    public PaymentView applyCredit(Long rentalId, CreditApplicationRequest request) {
        Rental rental = rental(rentalId);
        BigDecimal amount = Money.of(request.amount());
        requireWithinBalance(rental, amount);

        debtService.consumeCredit(rental.getCustomerId(), rentalId, amount,
                "Aplicado al alquiler " + rental.getCode());

        Payment payment = Payment.builder()
                .rentalId(rentalId)
                .paymentType(PaymentType.AJUSTE)
                .amount(amount)
                .status(PaymentStatus.CONFIRMADO)
                .paidAt(LocalDateTime.now())
                .note("Saldo a favor aplicado")
                .recordedBy(currentUser.idOrNull())
                .build();

        rental.setCreditApplied(Money.sum(rental.getCreditApplied(), amount));
        // El saldo a favor ya entro a caja en su momento, asi que no se asienta otra vez.
        Payment saved = paymentRepository.save(payment);
        afterPayment(rental, saved, false);

        return toView(saved, null);
    }

    // ------------------------------------------------------------- cierre

    /**
     * Cierra el alquiler despues de la liquidacion final: reparte el saldo entre
     * deuda, reembolso o saldo a favor y deja el alquiler en su estado final.
     */
    @Transactional
    public RentalAccount settle(Long rentalId, SettlementRequest request) {
        Rental rental = rental(rentalId);
        if (rental.getFinalAmount() == null) {
            throw new BusinessException("Primero registra la devolucion del material para liquidar");
        }
        if (rental.getStatus() != RentalStatus.FACTURACION_FINAL
                && rental.getStatus() != RentalStatus.PENDIENTE_PAGO) {
            throw BusinessException.of("El alquiler esta en estado %s y no se puede liquidar",
                    rental.getStatus());
        }

        BigDecimal total = Money.of(rental.getFinalAmount());
        BigDecimal paid = netPaid(rentalId);
        BigDecimal difference = Money.subtract(total, paid);

        if (difference.signum() < 0) {
            BigDecimal surplus = difference.abs();
            SurplusHandling handling = request == null || request.surplusHandling() == null
                    ? SurplusHandling.REEMBOLSO
                    : request.surplusHandling();
            returnSurplus(rental, surplus, handling, request == null ? null : request.note());
        } else if (difference.signum() > 0) {
            debtService.openOrUpdate(rental, difference, LocalDate.now().plusDays(DEBT_GRACE_DAYS));
            rentalService.transition(rental, RentalStatus.PENDIENTE_PAGO,
                    "Saldo pendiente de " + Money.format(difference));
            rentalRepository.save(rental);
            auditService.record("LIQUIDAR_ALQUILER", "Rental", rentalId,
                    "Queda debiendo " + Money.format(difference));
            return toAccount(rental);
        }

        close(rental);
        return toAccount(rental);
    }

    // -------------------------------------------------------------- internos

    /**
     * Devuelve al cliente lo que pago de mas, en efectivo o dejandoselo abonado.
     * En ambos casos el alquiler queda saldado exactamente en su valor final.
     */
    private void returnSurplus(Rental rental, BigDecimal surplus, SurplusHandling handling, String note) {
        boolean cash = handling == SurplusHandling.REEMBOLSO;

        Payment refund = paymentRepository.save(Payment.builder()
                .rentalId(rental.getId())
                .paymentType(PaymentType.REEMBOLSO)
                .amount(surplus)
                .status(PaymentStatus.CONFIRMADO)
                .paidAt(LocalDateTime.now())
                .note(cash ? appendNote("Devolucion en efectivo por devolucion anticipada", note)
                        : "Trasladado a saldo a favor del cliente")
                .recordedBy(currentUser.idOrNull())
                .build());

        if (cash) {
            rental.setRefundAmount(Money.sum(rental.getRefundAmount(), surplus));
            financeService.reconcilePayment(refund.getId(), rental.getId(), surplus,
                    MovementType.GASTO, "Reembolso al cliente por el alquiler " + rental.getCode());
        } else {
            debtService.grantCredit(rental.getCustomerId(), rental.getId(), surplus,
                    "Excedente del alquiler " + rental.getCode());
        }

        auditService.record("DEVOLVER_EXCEDENTE", "Rental", rental.getId(),
                "%s (%s)".formatted(Money.format(surplus), handling));
    }

    private void close(Rental rental) {
        rentalService.transition(rental, RentalStatus.CERRADO, "Alquiler pagado y cerrado");
        rentalRepository.save(rental);

        rentalService.registerSuccessfulClosure(rental);
        debtService.applyPayment(rental.getId(), Money.of(rental.getFinalAmount()));

        auditService.record("CERRAR_ALQUILER", "Rental", rental.getId(),
                Money.format(rental.getFinalAmount()));

        closureListener.ifPresent(listener -> {
            try {
                listener.onRentalClosed(rental);
            } catch (RuntimeException e) {
                // Un fallo enviando la factura no puede dejar el alquiler sin cerrar.
                log.warn("No se pudo notificar el cierre del alquiler {}", rental.getCode(), e);
            }
        });
    }

    /** Guarda el pago, lo asienta en caja y avanza el alquiler si quedo saldado. */
    private Payment confirmAndPost(Rental rental, Payment payment) {
        Payment saved = paymentRepository.save(payment);
        afterPayment(rental, saved, true);
        return saved;
    }

    private void afterPayment(Rental rental, Payment payment, boolean postToCash) {
        if (postToCash) {
            financeService.reconcilePayment(payment.getId(), rental.getId(), payment.getAmount(),
                    MovementType.INGRESO,
                    "Pago %s del alquiler %s".formatted(payment.getPaymentType(), rental.getCode()));
        }
        debtService.applyPayment(rental.getId(), payment.getAmount());

        auditService.record("REGISTRAR_PAGO", "Rental", rental.getId(),
                "%s %s".formatted(payment.getPaymentType(), Money.format(payment.getAmount())));

        // Si el alquiler ya estaba liquidado y este pago lo salda, se cierra solo.
        if (rental.getFinalAmount() != null && !Money.isPositive(balanceOf(rental))
                && (rental.getStatus() == RentalStatus.PENDIENTE_PAGO
                    || rental.getStatus() == RentalStatus.FACTURACION_FINAL)) {
            close(rental);
        } else {
            rentalRepository.save(rental);
        }
    }

    /** Tras anular un pago hay que reabrir la deuda con el saldo real. */
    private void recomputeDebt(Rental rental) {
        if (rental.getFinalAmount() == null) {
            return;
        }
        BigDecimal balance = balanceOf(rental);
        if (Money.isPositive(balance)) {
            debtService.openOrUpdate(rental, balance, LocalDate.now().plusDays(DEBT_GRACE_DAYS));
        }
    }

    private void requireWithinBalance(Rental rental, BigDecimal amount) {
        if (rental.getStatus() == RentalStatus.CANCELADO) {
            throw new BusinessException("No se reciben pagos de un alquiler cancelado");
        }
        BigDecimal balance = balanceOf(rental);
        if (!Money.isPositive(balance)) {
            throw new BusinessException("Este alquiler ya esta pagado en su totalidad");
        }
        if (Money.gt(amount, balance)) {
            throw BusinessException.of("El saldo del alquiler es %s; no se puede recibir %s",
                    Money.format(balance), Money.format(amount));
        }
    }

    private BigDecimal balanceOf(Rental rental) {
        return Money.subtract(rental.currentAmount(), netPaid(rental.getId()));
    }

    private BigDecimal netPaid(Long rentalId) {
        return Money.of(paymentRepository.netPaidForRental(rentalId));
    }

    private Rental rental(Long rentalId) {
        return rentalRepository.findById(rentalId)
                .orElseThrow(() -> ResourceNotFoundException.of("Alquiler", rentalId));
    }

    private Payment payment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Pago", paymentId));
    }

    private RentalAccount toAccount(Rental rental) {
        Customer customer = customerRepository.findById(rental.getCustomerId()).orElse(null);
        BigDecimal due = rental.currentAmount();
        BigDecimal paid = netPaid(rental.getId());
        BigDecimal difference = Money.subtract(due, paid);

        return new RentalAccount(rental.getId(), rental.getCode(), rental.getStatus().name(),
                rental.getCustomerId(), customer == null ? null : customer.getName(),
                rental.getEstimatedTotal(), rental.getFinalAmount(), due, paid,
                Money.atLeastZero(difference),
                Money.atLeastZero(difference.negate()),
                customer == null ? Money.ZERO : debtService.availableCredit(customer.getId()),
                rental.getCreditApplied(), rental.getRefundAmount(),
                rental.getFinalAmount() != null, !Money.isPositive(difference),
                paymentsOf(rental.getId()));
    }

    private PaymentView toView(Payment payment, String checkoutUrl) {
        String qrDataUri = payment.getQrContent() == null || !PaymentStatus.PENDIENTE.equals(payment.getStatus())
                ? null
                : qrCodeService.dataUri(payment.getQrContent(), 320);

        return new PaymentView(payment.getId(), payment.getRentalId(), payment.getPaymentType(),
                payment.getStatus(), payment.getAmount(), payment.getAmountTendered(),
                payment.getChangeGiven(), payment.getReference(), payment.getProvider(),
                payment.getNote(), qrDataUri, checkoutUrl, payment.getPaidAt(), payment.getCreatedAt());
    }

    private String appendNote(String existing, String addition) {
        if (addition == null || addition.isBlank()) {
            return existing;
        }
        return existing == null || existing.isBlank() ? addition.trim() : existing + " | " + addition.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
