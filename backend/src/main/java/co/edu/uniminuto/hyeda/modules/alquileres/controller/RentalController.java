package co.edu.uniminuto.hyeda.modules.alquileres.controller;

import co.edu.uniminuto.hyeda.modules.alquileres.dto.*;
import co.edu.uniminuto.hyeda.modules.alquileres.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alquileres")
@PreAuthorize("hasAnyAuthority('ALQUILERES','ADMIN')")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @GetMapping
    public List<RentalDto> list() {
        return rentalService.findAll();
    }

    @GetMapping("/{id}")
    public RentalDto get(@PathVariable Long id) {
        return rentalService.findById(id);
    }

    @GetMapping("/cliente/{customerId}")
    public List<RentalDto> byCustomer(@PathVariable Long customerId) {
        return rentalService.byCustomer(customerId);
    }

    @PostMapping
    public RentalDto create(@Valid @RequestBody CreateRentalRequest request) {
        return rentalService.create(request);
    }

    @PostMapping("/{id}/emitir-documento")
    public RentalDto emit(@PathVariable Long id) {
        return rentalService.emitDocument(id);
    }

    @PostMapping("/{id}/confirmar-entrega")
    public RentalDto deliver(@PathVariable Long id) {
        return rentalService.confirmDelivery(id);
    }

    @PostMapping("/{id}/extender")
    public RentalDto extend(@PathVariable Long id, @Valid @RequestBody ExtensionRequest request) {
        return rentalService.extend(id, request);
    }

    @PostMapping("/{id}/devolver")
    public RentalDto returnRental(@PathVariable Long id, @RequestBody ReturnRentalRequest request) {
        return rentalService.registerReturn(id, request);
    }

    @PostMapping("/{id}/cerrar")
    public RentalDto close(@PathVariable Long id) {
        return rentalService.closeIfPaid(id);
    }
}
