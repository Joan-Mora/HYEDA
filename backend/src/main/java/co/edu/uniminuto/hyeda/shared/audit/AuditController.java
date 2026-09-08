package co.edu.uniminuto.hyeda.shared.audit;

import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sistema/auditoria")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USUARIOS_GESTIONAR')")
public class AuditController {

    private final AuditService service;

    @GetMapping
    public PageResponse<AuditLog> recent(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId,
            @PageableDefault(size = 30) Pageable pageable) {
        if (entidad != null && entidadId != null) {
            return PageResponse.from(service.history(entidad, entidadId, pageable));
        }
        return PageResponse.from(service.recent(pageable));
    }
}
