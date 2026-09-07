package co.edu.uniminuto.hyeda.modules.clientes.controller;

import co.edu.uniminuto.hyeda.modules.clientes.dto.CustomerDto;
import co.edu.uniminuto.hyeda.modules.clientes.dto.CustomerRequest;
import co.edu.uniminuto.hyeda.modules.clientes.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@PreAuthorize("hasAnyAuthority('CLIENTES','ADMIN','ALQUILERES')")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<CustomerDto> list(@RequestParam(required = false) String q) {
        return customerService.findAll(q);
    }

    @GetMapping("/{id}")
    public CustomerDto get(@PathVariable Long id) {
        return customerService.findById(id);
    }

    @PostMapping
    public CustomerDto create(@Valid @RequestBody CustomerRequest request) {
        return customerService.create(request);
    }

    @PutMapping("/{id}")
    public CustomerDto update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @PostMapping("/{id}/cedula")
    public CustomerDto uploadId(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return customerService.uploadIdCopy(id, file);
    }
}
