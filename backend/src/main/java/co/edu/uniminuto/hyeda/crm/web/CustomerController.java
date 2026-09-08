package co.edu.uniminuto.hyeda.crm.web;

import co.edu.uniminuto.hyeda.crm.domain.CustomerType;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.CustomerProfile;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.CustomerView;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.SaveCustomerRequest;
import co.edu.uniminuto.hyeda.crm.service.CustomerService;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENTES_VER')")
    public PageResponse<CustomerView> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CustomerType tipo,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 25, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.search(q, tipo, activo, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_VER')")
    public CustomerView findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/perfil")
    @PreAuthorize("hasAuthority('CLIENTES_VER')")
    public CustomerProfile profile(@PathVariable Long id) {
        return service.profile(id);
    }

    @GetMapping("/documento/{documento}")
    @PreAuthorize("hasAuthority('CLIENTES_VER')")
    public CustomerView findByDocument(@PathVariable String documento) {
        return service.findByDocument(documento);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLIENTES_GESTIONAR')")
    public CustomerView create(@Valid @RequestBody SaveCustomerRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTES_GESTIONAR')")
    public CustomerView update(@PathVariable Long id, @Valid @RequestBody SaveCustomerRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('CLIENTES_GESTIONAR')")
    public CustomerView setActive(@PathVariable Long id, @RequestParam boolean activo) {
        return service.setActive(id, activo);
    }

    @PostMapping(value = "/{id}/cedula", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CLIENTES_GESTIONAR')")
    public CustomerView uploadIdCopy(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return service.uploadIdCopy(id, file);
    }

    @GetMapping("/{id}/cedula")
    @PreAuthorize("hasAuthority('CLIENTES_VER')")
    public ResponseEntity<byte[]> downloadIdCopy(@PathVariable Long id) {
        byte[] content = service.readIdCopy(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cedula-%d\"".formatted(id))
                .body(content);
    }
}
