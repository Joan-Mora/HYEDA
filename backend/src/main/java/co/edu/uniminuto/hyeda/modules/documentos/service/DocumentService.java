package co.edu.uniminuto.hyeda.modules.documentos.service;

import co.edu.uniminuto.hyeda.modules.alquileres.entity.DocumentType;
import co.edu.uniminuto.hyeda.modules.alquileres.entity.Rental;
import co.edu.uniminuto.hyeda.modules.alquileres.entity.RentalItem;
import co.edu.uniminuto.hyeda.modules.documentos.dto.DocumentDto;
import co.edu.uniminuto.hyeda.modules.documentos.entity.GeneratedDocument;
import co.edu.uniminuto.hyeda.modules.documentos.entity.GeneratedDocumentType;
import co.edu.uniminuto.hyeda.modules.documentos.repository.GeneratedDocumentRepository;
import co.edu.uniminuto.hyeda.shared.exception.BusinessException;
import co.edu.uniminuto.hyeda.shared.exception.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.util.FileStorageService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DocumentService {

    private final GeneratedDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    public DocumentService(GeneratedDocumentRepository documentRepository,
                           FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<DocumentDto> findAll() {
        return documentRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<DocumentDto> byRental(Long rentalId) {
        return documentRepository.findByRentalId(rentalId).stream().map(this::toDto).toList();
    }

    public DocumentDto findById(Long id) {
        return toDto(documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado")));
    }

    public Path resolveFile(Long id) {
        GeneratedDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        return Path.of(doc.getFilePath());
    }

    @Transactional
    public DocumentDto generateForRental(Rental rental) {
        GeneratedDocumentType type = rental.getDocumentType() == DocumentType.REMISION
                || Boolean.TRUE.equals(rental.getSkipContract())
                ? GeneratedDocumentType.REMISION
                : GeneratedDocumentType.CONTRATO;
        return generate(rental, type, buildHtml(rental, type));
    }

    @Transactional
    public DocumentDto generateReturnSlip(Rental rental) {
        return generate(rental, GeneratedDocumentType.DEVOLUCION, buildHtml(rental, GeneratedDocumentType.DEVOLUCION));
    }

    @Transactional
    public DocumentDto generateInvoice(Rental rental) {
        return generate(rental, GeneratedDocumentType.FACTURA, buildHtml(rental, GeneratedDocumentType.FACTURA));
    }

    @Transactional
    public DocumentDto generateReport(String title, String htmlBody) {
        String serial = nextSerial(GeneratedDocumentType.REPORTE);
        Path out = fileStorageService.documentsDir().resolve(serial + ".pdf");
        writePdf(htmlBody, out);
        GeneratedDocument doc = GeneratedDocument.builder()
                .documentType(GeneratedDocumentType.REPORTE)
                .serialNumber(serial)
                .filePath(out.toString())
                .metadataJson("{\"title\":\"" + title + "\"}")
                .build();
        return toDto(documentRepository.save(doc));
    }

    private DocumentDto generate(Rental rental, GeneratedDocumentType type, String html) {
        String serial = nextSerial(type);
        Path out = fileStorageService.documentsDir().resolve(serial + ".pdf");
        writePdf(html, out);
        GeneratedDocument doc = GeneratedDocument.builder()
                .rental(rental)
                .documentType(type)
                .serialNumber(serial)
                .filePath(out.toString())
                .build();
        return toDto(documentRepository.save(doc));
    }

    private String nextSerial(GeneratedDocumentType type) {
        long n = documentRepository.countByDocumentType(type) + 1;
        String prefix = switch (type) {
            case CONTRATO -> "CTR";
            case REMISION -> "REM";
            case DEVOLUCION -> "DEV";
            case FACTURA -> "FAC";
            case REPORTE -> "REP";
        };
        return prefix + "-" + LocalDate.now().getYear() + "-" + String.format("%05d", n);
    }

    private void writePdf(String html, Path out) {
        try (OutputStream os = Files.newOutputStream(out)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
        } catch (Exception e) {
            throw new BusinessException("Error generando PDF: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String buildHtml(Rental rental, GeneratedDocumentType type) {
        StringBuilder items = new StringBuilder();
        for (RentalItem item : rental.getItems()) {
            items.append("<tr><td>").append(esc(item.getProduct().getName()))
                    .append("</td><td>").append(item.getQuantity())
                    .append("</td><td>").append(item.getUnitPrice())
                    .append("</td></tr>");
        }
        String title = switch (type) {
            case CONTRATO -> "CONTRATO DE ALQUILER";
            case REMISION -> "REMISION DE ENTREGA";
            case DEVOLUCION -> "ACTA DE DEVOLUCION";
            case FACTURA -> "FACTURA DE ALQUILER";
            default -> "DOCUMENTO HYEDA";
        };
        String amount = rental.getFinalAmount() != null
                ? rental.getFinalAmount().toPlainString()
                : rental.getTotalAmount().toPlainString();
        String style = "body{font-family:Arial,sans-serif;font-size:12px;color:#222;margin:24px}"
                + "h1{font-size:18px;color:#c45c26;margin-bottom:4px}"
                + ".meta{color:#555;margin-bottom:16px}"
                + "table{width:100%;border-collapse:collapse;margin-top:12px}"
                + "th,td{border:1px solid #ccc;padding:6px;text-align:left}"
                + "th{background:#f3f0ec}"
                + ".foot{margin-top:24px;font-size:11px;color:#666}";
        return "<html><head><style>" + style + "</style></head><body>"
                + "<h1>HYEDA - Herramientas y Equipos del Ariari</h1>"
                + "<div class=\"meta\">NIT 000.000.000-0 | Granada, Meta | " + type.name() + "</div>"
                + "<h2>" + title + "</h2>"
                + "<p><b>Cliente:</b> " + esc(rental.getCustomer().getName())
                + " (" + esc(rental.getCustomer().getDocument()) + ")<br/>"
                + "<b>Referencia:</b> "
                + esc(rental.getDeliveryReference() == null ? "-" : rental.getDeliveryReference()) + "<br/>"
                + "<b>Direccion entrega:</b> "
                + esc(rental.getDeliveryAddress() == null ? "-" : rental.getDeliveryAddress()) + "<br/>"
                + "<b>Fechas:</b> " + rental.getStartDate() + " a "
                + (rental.getActualReturnDate() != null ? rental.getActualReturnDate() : rental.getEndDate()) + "<br/>"
                + "<b>Total:</b> $ " + amount + "</p>"
                + "<table><thead><tr><th>Producto</th><th>Cant.</th><th>Precio/dia</th></tr></thead>"
                + "<tbody>" + items + "</tbody></table>"
                + "<div class=\"foot\">Documento generado automaticamente el "
                + LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                + ". Valido como soporte operativo HYEDA.</div>"
                + "</body></html>";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private DocumentDto toDto(GeneratedDocument d) {
        return new DocumentDto(
                d.getId(),
                d.getRental() == null ? null : d.getRental().getId(),
                d.getDocumentType(),
                d.getSerialNumber(),
                d.getFilePath(),
                d.getGeneratedAt()
        );
    }
}
