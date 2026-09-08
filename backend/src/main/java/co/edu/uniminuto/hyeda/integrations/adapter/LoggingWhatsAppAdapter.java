package co.edu.uniminuto.hyeda.integrations.adapter;

import co.edu.uniminuto.hyeda.integrations.port.WhatsAppPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Canal de WhatsApp apagado: deja constancia en el log en vez de enviar.
 *
 * <p>Es el comportamiento por defecto para que el sistema funcione completo sin
 * credenciales de Meta. Los mensajes quedan igualmente en la bandeja de salida,
 * de modo que se puede revisar exactamente que se habria enviado.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "hyeda.integrations.whatsapp.enabled", havingValue = "false",
        matchIfMissing = true)
public class LoggingWhatsAppAdapter implements WhatsAppPort {

    @Override
    public SendResult sendText(String recipient, String message) {
        log.info("[WhatsApp simulado] Para {}:\n{}", recipient, message);
        return SendResult.ok("{\"simulado\":true}");
    }

    @Override
    public SendResult sendDocument(String recipient, String caption, Path documentPath, String fileName) {
        log.info("[WhatsApp simulado] Para {} con adjunto {} ({}):\n{}",
                recipient, fileName, documentPath, caption);
        return SendResult.ok("{\"simulado\":true,\"adjunto\":\"%s\"}".formatted(fileName));
    }

    @Override
    public boolean enabled() {
        return false;
    }
}
