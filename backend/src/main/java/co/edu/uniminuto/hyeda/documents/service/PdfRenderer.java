package co.edu.uniminuto.hyeda.documents.service;

import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;

/**
 * Convierte una plantilla Thymeleaf en PDF.
 *
 * <p>La plantilla debe ser XHTML valido: OpenHTMLToPDF no tolera etiquetas sin
 * cerrar. Por eso todas las plantillas de {@code templates/documents} cierran
 * explicitamente {@code <br/>}, {@code <img/>} y demas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfRenderer {

    private final TemplateEngine templateEngine;

    public byte[] render(String templatePath, Map<String, Object> model) {
        Context context = new Context(Locale.of("es", "CO"));
        context.setVariables(model);

        String html = templateEngine.process(templatePath, context);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception e) {
            log.error("Fallo la generacion del PDF con la plantilla {}", templatePath, e);
            throw new BusinessException("No se pudo generar el PDF: " + e.getMessage());
        }
    }
}
