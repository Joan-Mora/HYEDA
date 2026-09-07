package co.edu.uniminuto.hyeda.modules.inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "daily_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal dailyPrice;

    @Column(name = "stock_total", nullable = false)
    @Builder.Default
    private Integer stockTotal = 0;

    @Column(name = "stock_available", nullable = false)
    @Builder.Default
    private Integer stockAvailable = 0;

    @Column(name = "min_stock_threshold")
    @Builder.Default
    private Integer minStockThreshold = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Version
    private Long version;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (stockTotal == null) stockTotal = 0;
        if (stockAvailable == null) stockAvailable = stockTotal;
        if (active == null) active = true;
        if (minStockThreshold == null) minStockThreshold = 1;
    }
}
