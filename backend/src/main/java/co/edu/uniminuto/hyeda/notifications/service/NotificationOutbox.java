package co.edu.uniminuto.hyeda.notifications.service;

import co.edu.uniminuto.hyeda.integrations.port.WhatsAppPort;
import co.edu.uniminuto.hyeda.notifications.domain.Notification;
import co.edu.uniminuto.hyeda.notifications.domain.NotificationStatus;
import co.edu.uniminuto.hyeda.notifications.domain.NotificationTemplate;
import co.edu.uniminuto.hyeda.notifications.repository.NotificationRepository;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Bandeja de salida de mensajes.
 *
 * <p>Encolar y enviar estan separados a proposito: la operacion de negocio solo
 * guarda el mensaje (rapido y transaccional) y un job lo despacha despues. Asi
 * un WhatsApp caido nunca tumba un alquiler ni un pago.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutbox {

    private static final int BATCH_SIZE = 20;

    private final NotificationRepository repository;
    private final WhatsAppPort whatsApp;
    private final FileStorageService fileStorage;

    /**
     * Encola un mensaje. Se ejecuta en transaccion propia para que el mensaje
     * quede guardado aunque la operacion que lo origino termine revirtiendose.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification enqueue(NotificationTemplate template, String rawRecipient, String message,
                                Long rentalId, Long customerId, String mediaPath) {
        String recipient = PhoneNumbers.toWhatsApp(rawRecipient);

        Notification notification = Notification.builder()
                .templateCode(template)
                .recipient(recipient == null ? String.valueOf(rawRecipient) : recipient)
                .message(message)
                .rentalId(rentalId)
                .customerId(customerId)
                .mediaPath(mediaPath)
                .status(recipient == null ? NotificationStatus.DESCARTADA : NotificationStatus.EN_COLA)
                .lastError(recipient == null ? "El destinatario no tiene un numero valido de WhatsApp" : null)
                .build();

        return repository.save(notification);
    }

    /** Despacha el siguiente lote pendiente. Lo invoca el job cada minuto. */
    @Transactional
    public int dispatchPending() {
        List<Notification> pending = repository.findPending(
                Notification.MAX_ATTEMPTS, PageRequest.of(0, BATCH_SIZE));

        int sent = 0;
        for (Notification notification : pending) {
            if (send(notification)) {
                sent++;
            }
        }
        return sent;
    }

    @Transactional
    public Notification retry(Long id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Notificacion", id));
        notification.setStatus(NotificationStatus.EN_COLA);
        notification.setAttempts(0);
        notification.setLastError(null);
        repository.save(notification);
        send(notification);
        return notification;
    }

    private boolean send(Notification notification) {
        notification.setAttempts(notification.getAttempts() + 1);

        WhatsAppPort.SendResult result;
        try {
            result = notification.getMediaPath() == null
                    ? whatsApp.sendText(notification.getRecipient(), notification.getMessage())
                    : whatsApp.sendDocument(notification.getRecipient(), notification.getMessage(),
                            fileStorage.resolve(notification.getMediaPath()),
                            fileNameOf(notification.getMediaPath()));
        } catch (RuntimeException e) {
            result = WhatsAppPort.SendResult.failed(e.getMessage());
        }

        if (result.success()) {
            notification.setStatus(NotificationStatus.ENVIADA);
            notification.setSentAt(LocalDateTime.now());
            notification.setProviderResponse(truncate(result.providerResponse(), 4000));
            notification.setLastError(null);
        } else {
            notification.setLastError(truncate(result.error(), 1000));
            // Se agota la cola solo cuando ya no quedan reintentos.
            notification.setStatus(notification.canRetry()
                    ? NotificationStatus.EN_COLA
                    : NotificationStatus.FALLIDA);
            log.warn("No se pudo enviar la notificacion {} (intento {}): {}",
                    notification.getId(), notification.getAttempts(), result.error());
        }

        repository.save(notification);
        return result.success();
    }

    private String fileNameOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
