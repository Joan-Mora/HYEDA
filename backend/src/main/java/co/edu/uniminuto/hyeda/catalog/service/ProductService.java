package co.edu.uniminuto.hyeda.catalog.service;

import co.edu.uniminuto.hyeda.catalog.domain.Product;
import co.edu.uniminuto.hyeda.catalog.dto.ProductDtos.ProductView;
import co.edu.uniminuto.hyeda.catalog.dto.ProductDtos.SaveProductRequest;
import co.edu.uniminuto.hyeda.catalog.dto.ProductDtos.StockLedgerView;
import co.edu.uniminuto.hyeda.catalog.repository.ProductRepository;
import co.edu.uniminuto.hyeda.catalog.repository.StockLedgerRepository;
import co.edu.uniminuto.hyeda.shared.audit.AuditService;
import co.edu.uniminuto.hyeda.shared.domain.Money;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockLedgerRepository ledgerRepository;
    private final StockService stockService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<ProductView> search(String query, String category, Boolean active, Pageable pageable) {
        String normalized = (query == null || query.isBlank()) ? null : query.trim();
        String categoryFilter = (category == null || category.isBlank()) ? null : category;
        return PageResponse.from(
                productRepository.search(normalized, categoryFilter, active, pageable), this::toView);
    }

    @Transactional(readOnly = true)
    public ProductView findById(Long id) {
        return toView(find(id));
    }

    @Transactional(readOnly = true)
    public List<ProductView> lowStock() {
        return productRepository.findLowStock().stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<String> categories() {
        return productRepository.findCategories();
    }

    @Transactional
    public ProductView create(SaveProductRequest request) {
        String sku = normalizeSku(request.sku());
        if (sku != null && productRepository.existsBySkuIgnoreCase(sku)) {
            throw BusinessException.of("Ya existe un equipo con el codigo '%s'", sku);
        }

        int total = request.stockTotal();
        Product product = Product.builder()
                .sku(sku)
                .name(request.name().trim())
                .description(blankToNull(request.description()))
                .category(blankToNull(request.category()))
                .unit(request.unit() == null || request.unit().isBlank() ? "UNIDAD" : request.unit().trim())
                .dailyPrice(Money.of(request.dailyPrice()))
                .lateFeePerDay(defaultLateFee(request))
                .replacementValue(Money.of(request.replacementValue()))
                .stockTotal(total)
                .stockAvailable(total)
                .minStockThreshold(request.minStockThreshold() == null ? 1 : request.minStockThreshold())
                .active(request.active() == null || request.active())
                .build();

        Product saved = productRepository.save(product);
        if (total > 0) {
            stockService.registerImport(saved, total, "Alta de equipo en el patio");
        }
        auditService.record("CREAR_PRODUCTO", "Product", saved.getId(), saved.getName());
        return toView(saved);
    }

    @Transactional
    public ProductView update(Long id, SaveProductRequest request) {
        Product product = find(id);

        String sku = normalizeSku(request.sku());
        if (sku != null && !sku.equalsIgnoreCase(product.getSku())
                && productRepository.existsBySkuIgnoreCase(sku)) {
            throw BusinessException.of("Ya existe un equipo con el codigo '%s'", sku);
        }

        product.setSku(sku);
        product.setName(request.name().trim());
        product.setDescription(blankToNull(request.description()));
        product.setCategory(blankToNull(request.category()));
        if (request.unit() != null && !request.unit().isBlank()) {
            product.setUnit(request.unit().trim());
        }
        product.setDailyPrice(Money.of(request.dailyPrice()));
        product.setLateFeePerDay(defaultLateFee(request));
        product.setReplacementValue(Money.of(request.replacementValue()));
        if (request.minStockThreshold() != null) {
            product.setMinStockThreshold(request.minStockThreshold());
        }
        if (request.active() != null) {
            if (!request.active() && product.inUse() > 0) {
                throw new BusinessException(
                        "No puedes desactivar un equipo que tiene unidades en la calle");
            }
            product.setActive(request.active());
        }

        // El total solo se mueve por ajuste, que exige motivo y deja asiento contable.
        if (request.stockTotal() != null && !request.stockTotal().equals(product.getStockTotal())) {
            productRepository.save(product);
            Product adjusted = stockService.adjust(id, request.stockTotal(), "Ajuste desde edicion de equipo");
            auditService.record("EDITAR_PRODUCTO", "Product", id, adjusted.getName());
            return toView(adjusted);
        }

        Product saved = productRepository.save(product);
        auditService.record("EDITAR_PRODUCTO", "Product", id, saved.getName());
        return toView(saved);
    }

    @Transactional
    public ProductView adjustStock(Long id, int newTotal, String reason) {
        Product product = stockService.adjust(id, newTotal, reason);
        auditService.record("AJUSTAR_STOCK", "Product", id, "Nuevo total %d: %s".formatted(newTotal, reason));
        return toView(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockLedgerView> ledger(Long productId, Pageable pageable) {
        find(productId);
        return PageResponse.from(
                ledgerRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable),
                entry -> new StockLedgerView(entry.getId(), entry.getProductId(), entry.getRentalId(),
                        entry.getMovementType(), entry.getQuantity(), entry.getBalanceAfter(),
                        entry.getNote(), entry.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", id));
    }

    public ProductView toView(Product product) {
        return new ProductView(product.getId(), product.getSku(), product.getName(), product.getDescription(),
                product.getCategory(), product.getUnit(), product.getDailyPrice(), product.getLateFeePerDay(),
                product.getReplacementValue(), product.getStockTotal(), product.getStockAvailable(),
                product.inUse(), product.getMinStockThreshold(), product.isLowStock(), product.isOutOfStock(),
                Boolean.TRUE.equals(product.getActive()), product.getCreatedAt());
    }

    /**
     * Si no se define recargo por mora, se cobra el mismo precio del dia. Es lo
     * que la empresa hace en la practica cuando el cliente se pasa de la fecha.
     */
    private BigDecimal defaultLateFee(SaveProductRequest request) {
        return request.lateFeePerDay() == null || Money.isZero(request.lateFeePerDay())
                ? Money.of(request.dailyPrice())
                : Money.of(request.lateFeePerDay());
    }

    private String normalizeSku(String sku) {
        return sku == null || sku.isBlank() ? null : sku.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
