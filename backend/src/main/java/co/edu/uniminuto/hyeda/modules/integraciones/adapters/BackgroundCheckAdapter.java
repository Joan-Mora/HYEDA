package co.edu.uniminuto.hyeda.modules.integraciones.adapters;

import co.edu.uniminuto.hyeda.config.HyedaProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BackgroundCheckAdapter {

    private final HyedaProperties properties;

    public BackgroundCheckAdapter(HyedaProperties properties) {
        this.properties = properties;
    }

    public Map<String, String> consult(String document) {
        String provider = properties.getBackgroundCheck().getProvider();
        if ("manual".equalsIgnoreCase(provider) || properties.getBackgroundCheck().getApiKey() == null
                || properties.getBackgroundCheck().getApiKey().isBlank()) {
            return Map.of(
                    "provider", "manual",
                    "result", "Consulta manual pendiente. Adjunte certificado PDF.",
                    "status", "PENDING_MANUAL"
            );
        }
        // Placeholder for Verifik/CheckID
        return Map.of(
                "provider", provider,
                "result", "Consulta enviada al proveedor para documento " + document,
                "status", "SUBMITTED"
        );
    }
}
