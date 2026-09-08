package co.edu.uniminuto.hyeda.documents.web;

import co.edu.uniminuto.hyeda.documents.dto.DocumentDtos.DocumentVerification;
import co.edu.uniminuto.hyeda.documents.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verificacion de documentos sin iniciar sesion: es el destino del QR impreso
 * en contratos y remisiones. Solo confirma autenticidad, nunca entrega el PDF.
 */
@RestController
@RequestMapping("/api/publico/documentos")
@RequiredArgsConstructor
public class PublicDocumentController {

    private final DocumentService service;

    @GetMapping("/verificar/{serial}")
    public DocumentVerification verify(@PathVariable String serial) {
        return service.verify(serial);
    }
}
