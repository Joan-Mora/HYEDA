package co.edu.uniminuto.hyeda.integrations.adapter;

import co.edu.uniminuto.hyeda.integrations.port.PaymentGatewayPort;
import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cobro por QR a traves de Wompi (checkout web con firma de integridad).
 *
 * <p>Se activa con {@code hyeda.integrations.payments.provider=wompi}. Wompi
 * trabaja en centavos y exige una firma SHA-256 sobre referencia, monto, moneda
 * y llave de integridad; si el enlace no la lleva, el checkout la rechaza.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hyeda.integrations.payments.provider", havingValue = "wompi")
public class WompiPaymentGatewayAdapter implements PaymentGatewayPort {

    private static final String CHECKOUT_URL = "https://checkout.wompi.co/p/";

    private final HyedaProperties properties;

    @Override
    public ChargeIntent createCharge(String reference, BigDecimal amount, String description) {
        HyedaProperties.Payments payments = properties.getIntegrations().getPayments();
        HyedaProperties.Wompi wompi = payments.getWompi();

        if (wompi.getPublicKey() == null || wompi.getIntegrityKey() == null) {
            throw new BusinessException(
                    "Faltan las llaves de Wompi en la configuracion; usa efectivo o transferencia");
        }

        long cents = amount.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2).longValueExact();
        String currency = payments.getCurrency();
        String signature = sha256(reference + cents + currency + wompi.getIntegrityKey());

        String checkoutUrl = "%s?public-key=%s&currency=%s&amount-in-cents=%d&reference=%s&signature:integrity=%s"
                .formatted(CHECKOUT_URL, wompi.getPublicKey(), currency, cents, reference, signature);

        return new ChargeIntent(reference, checkoutUrl, checkoutUrl,
                "{\"provider\":\"wompi\",\"reference\":\"%s\",\"amountInCents\":%d}"
                        .formatted(reference, cents));
    }

    @Override
    public String provider() {
        return "wompi";
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append("%02x".formatted(b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
