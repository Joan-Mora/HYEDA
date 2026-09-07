package co.edu.uniminuto.hyeda.modules.inventario.service;

import co.edu.uniminuto.hyeda.modules.inventario.dto.ProductDto;
import co.edu.uniminuto.hyeda.modules.inventario.dto.ProductRequest;
import co.edu.uniminuto.hyeda.modules.inventario.entity.Product;
import co.edu.uniminuto.hyeda.modules.inventario.repository.ProductRepository;
import co.edu.uniminuto.hyeda.shared.exception.BusinessException;
import co.edu.uniminuto.hyeda.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDto> findAll(String q) {
        List<Product> products = (q != null && !q.isBlank())
                ? productRepository.findByNameContainingIgnoreCase(q)
                : productRepository.findAll();
        return products.stream().map(this::toDto).toList();
    }

    public List<ProductDto> lowStock() {
        return productRepository.findLowStock().stream().map(this::toDto).toList();
    }

    public ProductDto findById(Long id) {
        return toDto(get(id));
    }

    @Transactional
    public ProductDto create(ProductRequest request) {
        Product p = Product.builder()
                .name(request.name())
                .description(request.description())
                .dailyPrice(request.dailyPrice())
                .stockTotal(request.stockTotal())
                .stockAvailable(request.stockTotal())
                .minStockThreshold(request.minStockThreshold() == null ? 1 : request.minStockThreshold())
                .active(request.active() == null || request.active())
                .build();
        return toDto(productRepository.save(p));
    }

    @Transactional
    public ProductDto update(Long id, ProductRequest request) {
        Product p = get(id);
        int delta = request.stockTotal() - p.getStockTotal();
        p.setName(request.name());
        p.setDescription(request.description());
        p.setDailyPrice(request.dailyPrice());
        p.setStockTotal(request.stockTotal());
        p.setStockAvailable(Math.max(0, p.getStockAvailable() + delta));
        if (request.minStockThreshold() != null) p.setMinStockThreshold(request.minStockThreshold());
        if (request.active() != null) p.setActive(request.active());
        return toDto(productRepository.save(p));
    }

    @Transactional
    public void reserveStock(Product product, int qty) {
        if (product.getStockAvailable() < qty) {
            throw new BusinessException("Stock insuficiente para " + product.getName());
        }
        product.setStockAvailable(product.getStockAvailable() - qty);
        productRepository.save(product);
    }

    @Transactional
    public void releaseStock(Product product, int qty) {
        product.setStockAvailable(product.getStockAvailable() + qty);
        productRepository.save(product);
    }

    public Product get(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    private ProductDto toDto(Product p) {
        boolean low = p.getStockAvailable() <= (p.getMinStockThreshold() == null ? 1 : p.getMinStockThreshold());
        return new ProductDto(p.getId(), p.getName(), p.getDescription(), p.getDailyPrice(),
                p.getStockTotal(), p.getStockAvailable(), p.getMinStockThreshold(), p.getActive(), low);
    }
}
