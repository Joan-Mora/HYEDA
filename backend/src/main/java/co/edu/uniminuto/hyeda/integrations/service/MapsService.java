package co.edu.uniminuto.hyeda.integrations.service;

import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Enlaces de Google Maps para la direccion de la obra.
 *
 * <p>Se generan en el backend para que el mismo enlace quede en el documento
 * PDF, en el mensaje de WhatsApp y en la ficha del alquiler.
 */
@Service
@RequiredArgsConstructor
public class MapsService {

    private final HyedaProperties properties;

    public boolean isConfigured() {
        String key = properties.getIntegrations().getMaps().getApiKey();
        return key != null && !key.isBlank();
    }

    public String apiKey() {
        return properties.getIntegrations().getMaps().getApiKey();
    }

    /**
     * Enlace compartible. Con coordenadas apunta al punto exacto; sin ellas,
     * a la busqueda por texto de la direccion.
     */
    public String buildShareUrl(String address, Double lat, Double lng) {
        if (lat != null && lng != null) {
            return "https://www.google.com/maps/search/?api=1&query=%s,%s".formatted(lat, lng);
        }
        if (address == null || address.isBlank()) {
            return null;
        }
        return "https://www.google.com/maps/search/?api=1&query=" + encode(address + ", Granada, Meta, Colombia");
    }

    /** URL para el iframe embebido; requiere API key. */
    public String buildEmbedUrl(String address, Double lat, Double lng) {
        if (!isConfigured()) {
            return null;
        }
        String query = (lat != null && lng != null)
                ? lat + "," + lng
                : encode(address == null ? "Granada, Meta" : address + ", Granada, Meta, Colombia");
        return "https://www.google.com/maps/embed/v1/place?key=%s&q=%s".formatted(apiKey(), query);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
