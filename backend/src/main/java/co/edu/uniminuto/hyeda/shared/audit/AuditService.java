package co.edu.uniminuto.hyeda.shared.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniminuto.hyeda.shared.security.CurrentUser;

/**
 * Bitacora de acciones sensibles.
 *
 * <p>Se escribe en una transaccion independiente: si la operacion de negocio
 * termina revirtiendose, el rastro del intento se conserva igual.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final CurrentUser currentUser;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, Long entityId, String detail) {
        try {
            repository.save(AuditLog.builder()
                    .userId(currentUser.idOrNull())
                    .username(currentUser.usernameOrSystem())
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .detail(truncate(detail))
                    .build());
        } catch (Exception e) {
            // La auditoria nunca debe tumbar una operacion de negocio.
            log.warn("No se pudo registrar auditoria {} sobre {} {}", action, entityType, entityId, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> history(String entityType, Long entityId, Pageable pageable) {
        return repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> recent(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    private String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        return detail.length() <= 4000 ? detail : detail.substring(0, 3997) + "...";
    }
}
