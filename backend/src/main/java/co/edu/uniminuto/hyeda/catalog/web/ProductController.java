package co.edu.uniminuto.hyeda.catalog.web;

import co.edu.uniminuto.hyeda.catalog.dto.ProductDtos.AdjustStockRequest;
import co.edu.uniminuto.hyeda.catalog.dto.ProductDtos.ProductView;
import co.edu.uniminuto.hyeda.catalog.dto.ProductDtos.SaveProductRequest;
import co.edu.uniminuto.hyeda.catalog.dto.ProductDtos.StockLedgerView;
import co.edu.uniminuto.hyeda.catalog.service.ProductService;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTARIO_VER')")
    public PageResponse<ProductView> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.search(q, categoria, activo, pageable);
    }

    @GetMapping("/stock-bajo")
    @PreAuthorize("hasAuthority('INVENTARIO_VER')")
    public List<ProductView> lowStock() {
        return service.lowStock();
    }

    @GetMapping("/categorias")
    @PreAuthorize("hasAuthority('INVENTARIO_VER')")
    public List<String> categories() {
        return service.categories();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTARIO_VER')")
    public ProductView findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAuthority('INVENTARIO_VER')")
    public PageResponse<StockLedgerView> ledger(@PathVariable Long id,
                                                @PageableDefault(size = 30) Pageable pageable) {
        return service.ledger(id, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('INVENTARIO_GESTIONAR')")
    public ProductView create(@Valid @RequestBody SaveProductRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTARIO_GESTIONAR')")
    public ProductView update(@PathVariable Long id, @Valid @RequestBody SaveProductRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/ajustar-stock")
    @PreAuthorize("hasAuthority('INVENTARIO_GESTIONAR')")
    public ProductView adjustStock(@PathVariable Long id, @Valid @RequestBody AdjustStockRequest request) {
        return service.adjustStock(id, request.newTotal(), request.reason());
    }
}
