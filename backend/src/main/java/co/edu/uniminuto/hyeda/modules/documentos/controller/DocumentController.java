package co.edu.uniminuto.hyeda.modules.documentos.controller;

import co.edu.uniminuto.hyeda.modules.documentos.dto.DocumentDto;
import co.edu.uniminuto.hyeda.modules.documentos.service.DocumentService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@PreAuthorize("hasAnyAuthority('DOCUMENTOS','ADMIN','ALQUILERES','FACTURACION')")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentDto> list() {
        return documentService.findAll();
    }

    @GetMapping("/alquiler/{rentalId}")
    public List<DocumentDto> byRental(@PathVariable Long rentalId) {
        return documentService.byRental(rentalId);
    }

    @GetMapping("/{id}")
    public DocumentDto get(@PathVariable Long id) {
        return documentService.findById(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> pdf(@PathVariable Long id) {
        Path path = documentService.resolveFile(id);
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
