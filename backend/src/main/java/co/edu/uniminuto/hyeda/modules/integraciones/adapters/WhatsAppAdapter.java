package co.edu.uniminuto.hyeda.modules.integraciones.adapters;

import co.edu.uniminuto.hyeda.config.HyedaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class WhatsAppAdapter {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppAdapter.class);
    private final HyedaProperties properties;
    private final RestClient restClient = RestClient.create();

    public WhatsAppAdapter(HyedaProperties properties) {
        this.properties = properties;
    }

    public boolean sendText(String phone, String message) {
        if (!properties.getWhatsapp().isEnabled()) {
            log.info("[WhatsApp MOCK] to={} msg={}", phone, message);
            return true;
        }
        try {
            String url = "https://graph.facebook.com/v19.0/"
                    + properties.getWhatsapp().getPhoneNumberId() + "/messages";
            restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + properties.getWhatsapp().getToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "messaging_product", "whatsapp",
                            "to", phone,
                            "type", "text",
                            "text", Map.of("body", message)
                    ))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("WhatsApp send failed: {}", e.getMessage());
            return false;
        }
    }
}
