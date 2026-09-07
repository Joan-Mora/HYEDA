package co.edu.uniminuto.hyeda.modules.alquileres.entity;

import co.edu.uniminuto.hyeda.modules.inventario.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "rental_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "estimated_days")
    private Integer estimatedDays;

    @Column(name = "actual_days")
    private Integer actualDays;
}
