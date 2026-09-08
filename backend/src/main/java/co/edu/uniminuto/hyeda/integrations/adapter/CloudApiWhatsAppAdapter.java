package co.edu.uniminuto.hyeda.integrations.adapter;

import co.edu.uniminuto.hyeda.integrations.port.WhatsAppPort;
import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.util.Map;

/**
 * Envio real por la Cloud API de WhatsApp (Meta).
 *
 * <p>Los adjuntos van en dos pasos: primero se sube el PDF al endpoint de media
 * y despues se envia el mensaje referenciando el id devuelto. Meta no acepta
 * URLs locales, por eso hay que subir el archivo.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hyeda.integrations.whatsapp.enabled", havingValue = "true")
public class CloudApiWhatsAppAdapter implements WhatsAppPort {

    private final HyedaProperties.WhatsApp config;
    private final RestClient client;

    public CloudApiWhatsAppAdapter(HyedaProperties properties, RestClient.Builder builder) {
        this.config = properties.getIntegrations().getWhatsapp();
        this.client = builder
                .baseUrl("https://graph.facebook.com/%s".formatted(config.getApiVersion()))
                .defaultHeader("Authorization", "Bearer " + config.getToken())
                .build();
    }

    @Override
    public SendResult sendText(String recipient, String message) {
        try {
            String response = client.post()
                    .uri("/{phoneNumberId}/messages", config.getPhoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "messaging_product", "whatsapp",
                            "recipient_type", "individual",
                            "to", recipient,
                            "type", "text",
                            "text", Map.of("preview_url", false, "body", message)))
                    .retrieve()
                    .body(String.class);
            return SendResult.ok(response);
        } catch (Exception e) {
            log.warn("Fallo el envio de WhatsApp a {}", recipient, e);
            return SendResult.failed(e.getMessage());
        }
    }

    @Override
    public SendResult sendDocument(String recipient, String caption, Path documentPath, String fileName) {
        try {
            String mediaId = uploadMedia(documentPath);
            if (mediaId == null) {
                return SendResult.failed("Meta no devolvio el id del archivo subido");
            }

            String response = client.post()
                    .uri("/{phoneNumberId}/messages", config.getPhoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "messaging_product", "whatsapp",
                            "recipient_type", "individual",
                            "to", recipient,
                            "type", "document",
                            "document", Map.of("id", mediaId, "caption", caption, "filename", fileName)))
                    .retrieve()
                    .body(String.class);
            return SendResult.ok(response);
        } catch (Exception e) {
            log.warn("Fallo el envio del documento por WhatsApp a {}", recipient, e);
            return SendResult.failed(e.getMessage());
        }
    }

    @Override
    public boolean enabled() {
        return true;
    }

    private String uploadMedia(Path documentPath) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("messaging_product", "whatsapp");
        form.add("type", "application/pdf");
        form.add("file", new FileSystemResource(documentPath));

        @SuppressWarnings("unchecked")
        Map<String, Object> response = client.post()
                .uri("/{phoneNumberId}/media", config.getPhoneNumberId())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(Map.class);

        return response == null ? null : String.valueOf(response.get("id"));
    }
}
