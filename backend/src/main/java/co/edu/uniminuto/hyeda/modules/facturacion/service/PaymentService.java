package co.edu.uniminuto.hyeda.modules.facturacion.service;

import co.edu.uniminuto.hyeda.modules.alquileres.entity.Rental;
import co.edu.uniminuto.hyeda.modules.alquileres.service.RentalService;
import co.edu.uniminuto.hyeda.modules.facturacion.dto.PaymentDto;
import co.edu.uniminuto.hyeda.modules.facturacion.dto.PaymentRequest;
import co.edu.uniminuto.hyeda.modules.facturacion.entity.Payment;
import co.edu.uniminuto.hyeda.modules.facturacion.entity.PaymentStatus;
import co.edu.uniminuto.hyeda.modules.facturacion.entity.PaymentType;
import co.edu.uniminuto.hyeda.modules.facturacion.repository.PaymentRepository;
import co.edu.uniminuto.hyeda.modules.integraciones.adapters.PaymentGatewayAdapter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RentalService rentalService;
    private final PaymentGatewayAdapter paymentGatewayAdapter;

    public PaymentService(PaymentRepository paymentRepository,
                          @Lazy RentalService rentalService,
                          PaymentGatewayAdapter paymentGatewayAdapter) {
        this.paymentRepository = paymentRepository;
        this.rentalService = rentalService;
        this.paymentGatewayAdapter = paymentGatewayAdapter;
    }

    public List<PaymentDto> byRental(Long rentalId) {
        return paymentRepository.findByRentalId(rentalId).stream().map(this::toDto).toList();
    }

    @Transactional
    public PaymentDto register(PaymentRequest request) {
        Rental rental = rentalService.get(request.rentalId());
        Payment payment = Payment.builder()
                .rental(rental)
                .paymentType(request.paymentType())
                .amount(request.amount())
                .reference(request.reference())
                .status(PaymentStatus.PENDIENTE)
                .build();

        if (request.paymentType() == PaymentType.QR) {
            Map<String, String> qr = paymentGatewayAdapter.createQrPayment(rental.getId(), request.amount());
            payment.setReference(qr.getOrDefault("reference", request.reference()));
            payment.setStatus(PaymentStatus.PENDIENTE);
        } else {
            payment.setStatus(PaymentStatus.CONFIRMADO);
            payment.setPaidAt(Instant.now());
        }
        Payment saved = paymentRepository.save(payment);
        if (saved.getStatus() == PaymentStatus.CONFIRMADO) {
            rentalService.closeIfPaid(rental.getId());
        }
        return toDto(saved);
    }

    @Transactional
    public PaymentDto confirmQr(Long paymentId, String providerReference) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        if (paymentGatewayAdapter.confirmPayment(providerReference)) {
            payment.setStatus(PaymentStatus.CONFIRMADO);
            payment.setPaidAt(Instant.now());
            payment.setReference(providerReference);
            paymentRepository.save(payment);
            rentalService.closeIfPaid(payment.getRental().getId());
        }
        return toDto(payment);
    }

    public BigDecimal totalPaid(Long rentalId) {
        return paymentRepository.findByRentalId(rentalId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.CONFIRMADO)
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
    }

    private PaymentDto toDto(Payment p) {
        return new PaymentDto(p.getId(), p.getRental().getId(), p.getPaymentType(), p.getAmount(),
                p.getStatus(), p.getReference(), p.getPaidAt());
    }
}
