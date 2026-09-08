package co.edu.uniminuto.hyeda.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40, unique = true)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 4000)
    private String description;

    @Column(length = 80)
    private String category;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String unit = "UNIDAD";

    @Column(name = "daily_price", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal dailyPrice = BigDecimal.ZERO;

    /** Recargo diario cuando el cliente se pasa de la fecha pactada. */
    @Column(name = "late_fee_per_day", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal lateFeePerDay = BigDecimal.ZERO;

    /** Valor de reposicion, base para cobrar un faltante o un dano total. */
    @Column(name = "replacement_value", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal replacementValue = BigDecimal.ZERO;

    @Column(name = "stock_total", nullable = false)
    @Builder.Default
    private Integer stockTotal = 0;

    /** Unidades libres. Baja al reservar, no al despachar. */
    @Column(name = "stock_available", nullable = false)
    @Builder.Default
    private Integer stockAvailable = 0;

    @Column(name = "min_stock_threshold", nullable = false)
    @Builder.Default
    private Integer minStockThreshold = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Unidades que estan hoy en la calle o apartadas. */
    public int inUse() {
        return stockTotal - stockAvailable;
    }

    public boolean isLowStock() {
        return stockAvailable <= minStockThreshold;
    }

    public boolean isOutOfStock() {
        return stockAvailable <= 0;
    }
}
