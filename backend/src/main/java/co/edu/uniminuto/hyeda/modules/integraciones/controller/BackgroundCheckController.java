package co.edu.uniminuto.hyeda.modules.integraciones.controller;

import co.edu.uniminuto.hyeda.modules.clientes.entity.BackgroundCheck;
import co.edu.uniminuto.hyeda.modules.integraciones.service.BackgroundCheckService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/antecedentes")
@PreAuthorize("hasAnyAuthority('CLIENTES','ADMIN','ALQUILERES')")
public class BackgroundCheckController {

    private final BackgroundCheckService service;

    public BackgroundCheckController(BackgroundCheckService service) {
        this.service = service;
    }

    @GetMapping
    public List<BackgroundCheck> list() {
        return service.findAll();
    }

    @PostMapping("/consultar")
    public BackgroundCheck consult(@RequestBody Map<String, Object> body) {
        Long customerId = body.get("customerId") == null ? null : Long.valueOf(body.get("customerId").toString());
        String document = body.get("document") == null ? null : body.get("document").toString();
        return service.consult(customerId, document);
    }

    @PostMapping("/{id}/evidencia")
    public BackgroundCheck evidence(@PathVariable Long id,
                                    @RequestParam("file") MultipartFile file,
                                    @RequestParam(required = false) String observations) {
        return service.attachEvidence(id, file, observations);
    }

    @GetMapping("/cliente/{customerId}")
    public List<BackgroundCheck> byCustomer(@PathVariable Long customerId) {
        return service.byCustomer(customerId);
    }
}
