package co.edu.uniminuto.hyeda.modules.integraciones.adapters;

import co.edu.uniminuto.hyeda.config.HyedaProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentGatewayAdapter {

    private final HyedaProperties properties;

    public PaymentGatewayAdapter(HyedaProperties properties) {
        this.properties = properties;
    }

    public Map<String, String> createQrPayment(Long rentalId, BigDecimal amount) {
        String reference = "QR-" + rentalId + "-" + UUID.randomUUID().toString().substring(0, 8);
        // Mock / Wompi-ready payload
        return Map.of(
                "provider", properties.getPayments().getProvider(),
                "reference", reference,
                "amount", amount.toPlainString(),
                "qrPayload", "hyeda://pay/" + reference,
                "publicKey", nullToEmpty(properties.getPayments().getWompi().getPublicKey())
        );
    }

    public boolean confirmPayment(String reference) {
        return reference != null && !reference.isBlank();
    }

    private String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
