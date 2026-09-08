package co.edu.uniminuto.hyeda.integrations.adapter;

import co.edu.uniminuto.hyeda.integrations.port.BackgroundCheckPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Flujo manual: el sistema deja la consulta abierta y el operador sube el
 * certificado expedido por la Policia Nacional.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hyeda.integrations.background-check.provider",
        havingValue = "manual", matchIfMissing = true)
public class ManualBackgroundCheckAdapter implements BackgroundCheckPort {

    @Override
    public Result consult(String document) {
        log.info("Consulta manual de antecedentes para documento {}", document);
        return Result.pendingManual();
    }

    @Override
    public String providerName() {
        return "manual";
    }
}
