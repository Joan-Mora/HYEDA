package co.edu.uniminuto.hyeda.billing.web;

import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.CashPaymentRequest;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.ConfirmPaymentRequest;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.CreditApplicationRequest;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.PaymentView;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.QrPaymentRequest;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.RentalAccount;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.SettlementRequest;
import co.edu.uniminuto.hyeda.billing.dto.BillingDtos.TransferPaymentRequest;
import co.edu.uniminuto.hyeda.billing.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    @GetMapping("/alquiler/{rentalId}")
    @PreAuthorize("hasAuthority('ALQUILERES_VER')")
    public RentalAccount account(@PathVariable Long rentalId) {
        return service.account(rentalId);
    }

    @GetMapping("/alquiler/{rentalId}/historial")
    @PreAuthorize("hasAuthority('ALQUILERES_VER')")
    public List<PaymentView> payments(@PathVariable Long rentalId) {
        return service.paymentsOf(rentalId);
    }

    @PostMapping("/alquiler/{rentalId}/efectivo")
    @PreAuthorize("hasAuthority('PAGOS_REGISTRAR')")
    public PaymentView payCash(@PathVariable Long rentalId,
                               @Valid @RequestBody CashPaymentRequest request) {
        return service.payCash(rentalId, request);
    }

    @PostMapping("/alquiler/{rentalId}/transferencia")
    @PreAuthorize("hasAuthority('PAGOS_REGISTRAR')")
    public PaymentView payTransfer(@PathVariable Long rentalId,
                                   @Valid @RequestBody TransferPaymentRequest request) {
        return service.payTransfer(rentalId, request);
    }

    @PostMapping("/alquiler/{rentalId}/qr")
    @PreAuthorize("hasAuthority('PAGOS_REGISTRAR')")
    public PaymentView createQr(@PathVariable Long rentalId,
                                @Valid @RequestBody QrPaymentRequest request) {
        return service.createQrCharge(rentalId, request);
    }

    @PostMapping("/alquiler/{rentalId}/saldo-a-favor")
    @PreAuthorize("hasAuthority('PAGOS_REGISTRAR')")
    public PaymentView applyCredit(@PathVariable Long rentalId,
                                   @Valid @RequestBody CreditApplicationRequest request) {
        return service.applyCredit(rentalId, request);
    }

    @PostMapping("/alquiler/{rentalId}/sin-pago")
    @PreAuthorize("hasAuthority('PAGOS_REGISTRAR')")
    public RentalAccount continueWithoutPayment(@PathVariable Long rentalId,
                                                @RequestParam(required = false) String nota) {
        return service.continueWithoutPayment(rentalId, nota);
    }

    @PostMapping("/alquiler/{rentalId}/liquidar")
    @PreAuthorize("hasAuthority('PAGOS_REGISTRAR')")
    public RentalAccount settle(@PathVariable Long rentalId,
                                @RequestBody(required = false) SettlementRequest request) {
        return service.settle(rentalId, request);
    }

    @PostMapping("/{paymentId}/confirmar")
    @PreAuthorize("hasAuthority('PAGOS_REGISTRAR')")
    public PaymentView confirm(@PathVariable Long paymentId,
                               @RequestBody(required = false) ConfirmPaymentRequest request) {
        return service.confirm(paymentId, request);
    }

    @PostMapping("/{paymentId}/anular")
    @PreAuthorize("hasAuthority('PAGOS_REGISTRAR')")
    public PaymentView cancel(@PathVariable Long paymentId,
                              @RequestParam(required = false) String motivo) {
        return service.cancel(paymentId, motivo);
    }
}
