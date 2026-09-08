package co.edu.uniminuto.hyeda.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Mensaje saliente, con patron outbox: primero se persiste y despues un job lo
 * despacha. Si WhatsApp esta caido el mensaje no se pierde, se reintenta.
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    /** Tres intentos: mas alla, casi siempre el problema es el numero, no la red. */
    public static final int MAX_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rental_id")
    private Long rentalId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String channel = "WHATSAPP";

    @Enumerated(EnumType.STRING)
    @Column(name = "template_code", nullable = false, length = 80)
    private NotificationTemplate templateCode;

    @Column(nullable = false, length = 40)
    private String recipient;

    @Column(nullable = false, length = 4000)
    private String message;

    /** Ruta del PDF adjunto (factura, reporte diario). */
    @Column(name = "media_path", length = 500)
    private String mediaPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.EN_COLA;

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "provider_response", length = 4000)
    private String providerResponse;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean canRetry() {
        return attempts < MAX_ATTEMPTS;
    }
}
