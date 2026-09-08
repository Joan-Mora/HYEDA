package co.edu.uniminuto.hyeda.crm.web;

import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.BackgroundCheckRequest;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.BackgroundCheckView;
import co.edu.uniminuto.hyeda.crm.dto.CustomerDtos.ResolveBackgroundCheckRequest;
import co.edu.uniminuto.hyeda.crm.service.BackgroundCheckService;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/antecedentes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ANTECEDENTES_CONSULTAR')")
public class BackgroundCheckController {

    private final BackgroundCheckService service;

    @GetMapping
    public PageResponse<BackgroundCheckView> list(@PageableDefault(size = 25) Pageable pageable) {
        return service.list(pageable);
    }

    @PostMapping("/consultar")
    public BackgroundCheckView consult(@Valid @RequestBody BackgroundCheckRequest request) {
        return service.consult(request);
    }

    @PutMapping("/{id}/resolver")
    public BackgroundCheckView resolve(@PathVariable Long id,
                                       @Valid @RequestBody ResolveBackgroundCheckRequest request) {
        return service.resolve(id, request);
    }

    @PostMapping(value = "/{id}/evidencia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BackgroundCheckView attachEvidence(@PathVariable Long id,
                                              @RequestPart("file") MultipartFile file) {
        return service.attachEvidence(id, file);
    }

    @GetMapping("/{id}/evidencia")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"antecedentes-%d.pdf\"".formatted(id))
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.readEvidence(id));
    }

    @GetMapping("/cliente/{customerId}")
    public List<BackgroundCheckView> byCustomer(@PathVariable Long customerId) {
        return service.byCustomer(customerId);
    }

    @GetMapping("/documento/{documento}")
    public List<BackgroundCheckView> byDocument(@PathVariable String documento) {
        return service.byDocument(documento);
    }
}
