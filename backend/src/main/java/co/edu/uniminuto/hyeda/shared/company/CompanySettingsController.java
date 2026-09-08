package co.edu.uniminuto.hyeda.shared.company;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sistema/empresa")
public class CompanySettingsController {

    private final CompanySettingsService service;

    public CompanySettingsController(CompanySettingsService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIGURACION_GESTIONAR')")
    public CompanySettings get() {
        return service.get();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('CONFIGURACION_GESTIONAR')")
    public CompanySettings update(@Valid @RequestBody CompanySettingsRequest request) {
        return service.update(request);
    }
}
