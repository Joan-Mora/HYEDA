package co.edu.uniminuto.hyeda.imports.web;

import co.edu.uniminuto.hyeda.imports.domain.ImportJobType;
import co.edu.uniminuto.hyeda.imports.dto.ImportDtos.ColumnMapping;
import co.edu.uniminuto.hyeda.imports.dto.ImportDtos.ImportJobView;
import co.edu.uniminuto.hyeda.imports.dto.ImportDtos.PreviewResponse;
import co.edu.uniminuto.hyeda.imports.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/importacion")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('IMPORTACION_EJECUTAR')")
public class ImportController {

    private final ImportService service;

    @PostMapping(value = "/previsualizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PreviewResponse preview(
            @RequestPart("file") MultipartFile file,
            @RequestParam ImportJobType tipo,
            @RequestPart(value = "mapeo", required = false) ColumnMapping mapping) {
        Map<String, String> map = mapping == null ? null : mapping.fieldToColumn();
        return service.preview(file, tipo, map);
    }

    @PostMapping(value = "/ejecutar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportJobView execute(
            @RequestPart("file") MultipartFile file,
            @RequestParam ImportJobType tipo,
            @RequestPart(value = "mapeo", required = false) ColumnMapping mapping) {
        Map<String, String> map = mapping == null ? null : mapping.fieldToColumn();
        return service.execute(file, tipo, map);
    }

    @GetMapping
    public List<ImportJobView> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ImportJobView find(@PathVariable Long id) {
        return service.find(id);
    }

    @GetMapping("/{id}/errores")
    public ResponseEntity<byte[]> errors(@PathVariable Long id) {
        byte[] csv = service.errorReport(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("importacion-%d-errores.csv".formatted(id)).build()
                                .toString())
                .body(csv);
    }
}
