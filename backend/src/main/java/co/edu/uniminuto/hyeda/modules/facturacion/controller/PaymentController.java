package co.edu.uniminuto.hyeda.modules.facturacion.controller;

import co.edu.uniminuto.hyeda.modules.facturacion.dto.PaymentDto;
import co.edu.uniminuto.hyeda.modules.facturacion.dto.PaymentRequest;
import co.edu.uniminuto.hyeda.modules.facturacion.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@PreAuthorize("hasAnyAuthority('FACTURACION','ADMIN','ALQUILERES')")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/alquiler/{rentalId}")
    public List<PaymentDto> byRental(@PathVariable Long rentalId) {
        return paymentService.byRental(rentalId);
    }

    @PostMapping
    public PaymentDto register(@Valid @RequestBody PaymentRequest request) {
        return paymentService.register(request);
    }

    @PostMapping("/{id}/confirmar-qr")
    public PaymentDto confirmQr(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return paymentService.confirmQr(id, body.getOrDefault("reference", "MOCK-OK"));
    }
}
