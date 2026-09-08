package co.edu.uniminuto.hyeda.documents.service;

import co.edu.uniminuto.hyeda.documents.domain.DocumentSequence;
import co.edu.uniminuto.hyeda.documents.domain.GeneratedDocumentType;
import co.edu.uniminuto.hyeda.documents.repository.DocumentSequenceRepository;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consecutivos legales de los documentos.
 *
 * <p>El contador se toma con bloqueo pesimista y en una transaccion propia: dos
 * operadores que generan contratos al mismo tiempo obtienen numeros distintos, y
 * el numero queda consumido aunque la generacion del PDF falle despues. Es
 * preferible un hueco en la numeracion a dos contratos con el mismo numero.
 */
@Service
@RequiredArgsConstructor
public class DocumentNumberService {

    private static final String FORMAT = "%s-%05d";

    private final DocumentSequenceRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(GeneratedDocumentType type) {
        DocumentSequence sequence = repository.findForUpdate(type)
                .orElseThrow(() -> BusinessException.of(
                        "No hay consecutivo configurado para documentos de tipo %s", type));

        long value = sequence.getNextValue();
        sequence.setNextValue(value + 1);
        repository.save(sequence);

        return FORMAT.formatted(sequence.getPrefix(), value);
    }
}
