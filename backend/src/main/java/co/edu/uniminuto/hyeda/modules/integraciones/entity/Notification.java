package co.edu.uniminuto.hyeda.modules.integraciones.entity;

import co.edu.uniminuto.hyeda.modules.alquileres.entity.Rental;
import co.edu.uniminuto.hyeda.modules.clientes.entity.Customer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id")
    private Rental rental;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(length = 40)
    private String channel;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 40)
    private String status;

    @Column(name = "sent_at")
    private Instant sentAt;
}
