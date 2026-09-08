package co.edu.uniminuto.hyeda.integrations.adapter;

import co.edu.uniminuto.hyeda.integrations.port.PaymentGatewayPort;
import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Cobro por QR sin pasarela contratada.
 *
 * <p>El QR apunta a una pagina propia con el detalle del cobro; el operador
 * confirma manualmente cuando ve la transferencia. Es el comportamiento que la
 * empresa necesita hoy y no bloquea el flujo del mostrador.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hyeda.integrations.payments.provider", havingValue = "mock",
        matchIfMissing = true)
public class MockPaymentGatewayAdapter implements PaymentGatewayPort {

    private final HyedaProperties properties;

    @Override
    public ChargeIntent createCharge(String reference, BigDecimal amount, String description) {
        String checkoutUrl = "%s/pagar/%s".formatted(baseUrl(), reference);
        String qrContent = "%s?monto=%s&concepto=%s".formatted(checkoutUrl,
                Money.of(amount).toPlainString(),
                URLEncoder.encode(description == null ? "Alquiler HYEDA" : description,
                        StandardCharsets.UTF_8));

        return new ChargeIntent(reference, qrContent, checkoutUrl,
                "{\"provider\":\"mock\",\"reference\":\"%s\",\"amount\":%s}"
                        .formatted(reference, Money.of(amount).toPlainString()));
    }

    @Override
    public String provider() {
        return "mock";
    }

    private String baseUrl() {
        String base = properties.getPublicBaseUrl();
        return base != null && base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
