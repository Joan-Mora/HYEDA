package co.edu.uniminuto.hyeda.modules.integraciones.controller;

import co.edu.uniminuto.hyeda.config.HyedaProperties;
import co.edu.uniminuto.hyeda.modules.integraciones.adapters.PaymentGatewayAdapter;
import co.edu.uniminuto.hyeda.modules.integraciones.adapters.WhatsAppAdapter;
import co.edu.uniminuto.hyeda.modules.integraciones.repository.NotificationRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integraciones")
@PreAuthorize("hasAnyAuthority('ADMIN','ALQUILERES','CLIENTES','FACTURACION')")
public class IntegracionesController {

    private final HyedaProperties properties;
    private final WhatsAppAdapter whatsAppAdapter;
    private final PaymentGatewayAdapter paymentGatewayAdapter;
    private final NotificationRepository notificationRepository;

    public IntegracionesController(HyedaProperties properties,
                                   WhatsAppAdapter whatsAppAdapter,
                                   PaymentGatewayAdapter paymentGatewayAdapter,
                                   NotificationRepository notificationRepository) {
        this.properties = properties;
        this.whatsAppAdapter = whatsAppAdapter;
        this.paymentGatewayAdapter = paymentGatewayAdapter;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("whatsapp", Map.of(
                "name", "WhatsApp Cloud API",
                "enabled", properties.getWhatsapp().isEnabled(),
                "configured", notBlank(properties.getWhatsapp().getToken())
                        && notBlank(properties.getWhatsapp().getPhoneNumberId()),
                "adminPhone", nullToEmpty(properties.getWhatsapp().getAdminPhone()),
                "mode", properties.getWhatsapp().isEnabled() ? "LIVE" : "MOCK",
                "description", "Notificaciones de alquiler, factura y reporte diario al admin."
        ));
        result.put("payments", Map.of(
                "name", "Pagos QR (Wompi / mock)",
                "provider", nullToEmpty(properties.getPayments().getProvider()),
                "configured", notBlank(properties.getPayments().getWompi().getPublicKey())
                        || "mock".equalsIgnoreCase(properties.getPayments().getProvider()),
                "mode", "mock".equalsIgnoreCase(properties.getPayments().getProvider()) ? "MOCK" : "LIVE",
                "description", "Cobros QR, efectivo y abonos vinculados al alquiler."
        ));
        String mapsKey = properties.getMaps().getApiKey();
        result.put("maps", Map.of(
                "name", "Google Maps",
                "configured", notBlank(mapsKey),
                "apiKey", notBlank(mapsKey) ? mapsKey : "",
                "mode", notBlank(mapsKey) ? "LIVE" : "PENDING",
                "description", "Geocodificación y mapa en dirección de entrega del wizard."
        ));
        result.put("backgroundCheck", Map.of(
                "name", "Antecedentes",
                "provider", nullToEmpty(properties.getBackgroundCheck().getProvider()),
                "configured", !"manual".equalsIgnoreCase(properties.getBackgroundCheck().getProvider())
                        && notBlank(properties.getBackgroundCheck().getApiKey()),
                "mode", "manual".equalsIgnoreCase(properties.getBackgroundCheck().getProvider()) ? "MANUAL" : "API",
                "description", "Consulta por cédula o adjunto de certificado PDF."
        ));
        return result;
    }

    @GetMapping("/maps-key")
    public Map<String, String> mapsKey() {
        String key = properties.getMaps().getApiKey();
        return Map.of(
                "configured", notBlank(key) ? "true" : "false",
                "apiKey", nullToEmpty(key)
        );
    }

    @GetMapping("/notificaciones")
    public List<Map<String, Object>> notifications() {
        return notificationRepository.findAll().stream()
                .map(n -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", n.getId());
                    row.put("channel", n.getChannel());
                    row.put("message", n.getMessage());
                    row.put("status", n.getStatus());
                    row.put("sentAt", n.getSentAt() != null ? n.getSentAt().toString() : null);
                    return row;
                })
                .toList();
    }

    @PostMapping("/whatsapp/test")
    public Map<String, Object> testWhatsApp(@RequestBody(required = false) Map<String, String> body) {
        String phone = body != null && body.get("phone") != null
                ? body.get("phone")
                : properties.getWhatsapp().getAdminPhone();
        String message = body != null && body.get("message") != null
                ? body.get("message")
                : "HYEDA: prueba de conexión WhatsApp.";
        boolean ok = whatsAppAdapter.sendText(phone, message);
        return Map.of("sent", ok, "phone", nullToEmpty(phone), "mode",
                properties.getWhatsapp().isEnabled() ? "LIVE" : "MOCK");
    }

    @PostMapping("/pagos/qr-demo")
    public Map<String, String> qrDemo(@RequestBody Map<String, Object> body) {
        Long rentalId = body.get("rentalId") == null ? 0L : Long.valueOf(body.get("rentalId").toString());
        BigDecimal amount = body.get("amount") == null
                ? BigDecimal.valueOf(10000)
                : new BigDecimal(body.get("amount").toString());
        return paymentGatewayAdapter.createQrPayment(rentalId, amount);
    }

    private boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }

    private String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
