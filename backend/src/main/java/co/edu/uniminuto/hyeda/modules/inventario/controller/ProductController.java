package co.edu.uniminuto.hyeda.modules.inventario.controller;

import co.edu.uniminuto.hyeda.modules.inventario.dto.ProductDto;
import co.edu.uniminuto.hyeda.modules.inventario.dto.ProductRequest;
import co.edu.uniminuto.hyeda.modules.inventario.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@PreAuthorize("hasAnyAuthority('INVENTARIO','ADMIN','ALQUILERES')")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductDto> list(@RequestParam(required = false) String q) {
        return productService.findAll(q);
    }

    @GetMapping("/low-stock")
    public List<ProductDto> lowStock() {
        return productService.lowStock();
    }

    @GetMapping("/{id}")
    public ProductDto get(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    public ProductDto create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductDto update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }
}
