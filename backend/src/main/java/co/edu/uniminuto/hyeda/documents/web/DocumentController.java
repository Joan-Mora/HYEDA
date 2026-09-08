package co.edu.uniminuto.hyeda.documents.web;

import co.edu.uniminuto.hyeda.documents.domain.GeneratedDocument;
import co.edu.uniminuto.hyeda.documents.domain.GeneratedDocumentType;
import co.edu.uniminuto.hyeda.documents.dto.DocumentDtos.DocumentRow;
import co.edu.uniminuto.hyeda.documents.service.DocumentService;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENTOS_VER')")
    public PageResponse<DocumentRow> search(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) GeneratedDocumentType tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 25, sort = "generatedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.search(buscar, tipo, desde, hasta, pageable);
    }

    @GetMapping("/alquiler/{rentalId}")
    @PreAuthorize("hasAuthority('DOCUMENTOS_VER')")
    public List<DocumentRow> byRental(@PathVariable Long rentalId) {
        return service.byRental(rentalId);
    }

    @GetMapping("/cliente/{customerId}")
    @PreAuthorize("hasAuthority('DOCUMENTOS_VER')")
    public List<DocumentRow> byCustomer(@PathVariable Long customerId) {
        return service.byCustomer(customerId);
    }

    /** Vista previa dentro de la aplicacion: el navegador lo abre en un visor embebido. */
    @GetMapping("/{id}/contenido")
    @PreAuthorize("hasAuthority('DOCUMENTOS_VER')")
    public ResponseEntity<byte[]> preview(@PathVariable Long id) {
        return pdf(id, false);
    }

    @GetMapping("/{id}/descargar")
    @PreAuthorize("hasAuthority('DOCUMENTOS_VER')")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        return pdf(id, true);
    }

    @PostMapping("/alquiler/{rentalId}/emitir")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public DocumentRow issue(@PathVariable Long rentalId) {
        return toRow(service.issueRentalDocument(rentalId));
    }

    @PostMapping("/alquiler/{rentalId}/devolucion")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public DocumentRow issueReturnReceipt(@PathVariable Long rentalId) {
        return toRow(service.issueReturnReceipt(rentalId));
    }

    @PostMapping("/alquiler/{rentalId}/factura")
    @PreAuthorize("hasAuthority('PAGOS_REGISTRAR')")
    public DocumentRow issueInvoice(@PathVariable Long rentalId) {
        return toRow(service.issueInvoice(rentalId));
    }

    @PostMapping("/{id}/firmar")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public DocumentRow markSigned(@PathVariable Long id) {
        return toRow(service.markSigned(id));
    }

    private ResponseEntity<byte[]> pdf(Long id, boolean attachment) {
        GeneratedDocument document = service.find(id);
        byte[] content = service.content(id);
        String fileName = document.getSerialNumber() + ".pdf";

        ContentDisposition disposition = (attachment
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename(fileName)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content);
    }

    private DocumentRow toRow(GeneratedDocument document) {
        return service.toRow(document);
    }
}
