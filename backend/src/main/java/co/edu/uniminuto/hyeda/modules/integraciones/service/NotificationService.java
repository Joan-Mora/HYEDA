package co.edu.uniminuto.hyeda.modules.integraciones.service;

import co.edu.uniminuto.hyeda.config.HyedaProperties;
import co.edu.uniminuto.hyeda.modules.alquileres.entity.Rental;
import co.edu.uniminuto.hyeda.modules.integraciones.adapters.WhatsAppAdapter;
import co.edu.uniminuto.hyeda.modules.integraciones.entity.Notification;
import co.edu.uniminuto.hyeda.modules.integraciones.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WhatsAppAdapter whatsAppAdapter;
    private final HyedaProperties properties;

    public NotificationService(NotificationRepository notificationRepository,
                               WhatsAppAdapter whatsAppAdapter,
                               HyedaProperties properties) {
        this.notificationRepository = notificationRepository;
        this.whatsAppAdapter = whatsAppAdapter;
        this.properties = properties;
    }

    @Transactional
    public void notifyRentalCreated(Rental rental) {
        String msg = "HYEDA: Alquiler #" + rental.getId() + " creado para "
                + rental.getCustomer().getName() + ". Total estimado: $" + rental.getTotalAmount();
        send(rental, rental.getCustomer().getPhone(), msg);
    }

    @Transactional
    public void notifyPaymentComplete(Rental rental) {
        String msg = "HYEDA: Pago completado para alquiler #" + rental.getId()
                + ". Factura disponible. Gracias.";
        send(rental, rental.getCustomer().getPhone(), msg);
    }

    @Transactional
    public void notifyAdmin(String message) {
        whatsAppAdapter.sendText(properties.getWhatsapp().getAdminPhone(), message);
        notificationRepository.save(Notification.builder()
                .channel("WHATSAPP")
                .message(message)
                .status("SENT")
                .sentAt(Instant.now())
                .build());
    }

    private void send(Rental rental, String phone, String message) {
        boolean ok = phone != null && whatsAppAdapter.sendText(phone, message);
        notificationRepository.save(Notification.builder()
                .rental(rental)
                .customer(rental.getCustomer())
                .channel("WHATSAPP")
                .message(message)
                .status(ok ? "SENT" : "FAILED")
                .sentAt(Instant.now())
                .build());
    }
}
