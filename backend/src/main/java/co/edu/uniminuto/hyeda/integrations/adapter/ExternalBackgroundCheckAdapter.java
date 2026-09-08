package co.edu.uniminuto.hyeda.integrations.adapter;

import co.edu.uniminuto.hyeda.integrations.port.BackgroundCheckPort;
import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Proveedor comercial de verificacion de identidad (Verifik, CheckID y
 * similares). Se activa con
 * {@code hyeda.integrations.background-check.provider=api}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "hyeda.integrations.background-check.provider", havingValue = "api")
public class ExternalBackgroundCheckAdapter implements BackgroundCheckPort {

    private final HyedaProperties properties;
    private final RestClient restClient = RestClient.create();

    @Override
    public Result consult(String document) {
        HyedaProperties.BackgroundCheck config = properties.getIntegrations().getBackgroundCheck();
        if (config.getApiKey() == null || config.getApiKey().isBlank()
                || config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            log.warn("Proveedor de antecedentes sin credenciales; la consulta queda manual");
            return Result.pendingManual();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(config.getBaseUrl() + "/antecedentes?documento=" + document)
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return Result.submitted("El proveedor no devolvio informacion");
            }
            boolean hasRecords = Boolean.TRUE.equals(response.get("tieneAntecedentes"));
            String summary = String.valueOf(response.getOrDefault("detalle", "Consulta procesada"));
            return new Result(true, hasRecords, summary);
        } catch (Exception e) {
            log.error("Fallo la consulta de antecedentes para {}: {}", document, e.getMessage());
            return Result.submitted("No se pudo contactar al proveedor: " + e.getMessage());
        }
    }

    @Override
    public String providerName() {
        return "api";
    }
}
