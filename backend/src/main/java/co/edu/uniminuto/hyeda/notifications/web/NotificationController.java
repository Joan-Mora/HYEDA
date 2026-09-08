package co.edu.uniminuto.hyeda.notifications.web;

import co.edu.uniminuto.hyeda.notifications.domain.Notification;
import co.edu.uniminuto.hyeda.notifications.domain.NotificationStatus;
import co.edu.uniminuto.hyeda.notifications.domain.NotificationTemplate;
import co.edu.uniminuto.hyeda.notifications.service.NotificationService;
import co.edu.uniminuto.hyeda.shared.web.PageResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    @PreAuthorize("hasAuthority('NOTIFICACIONES_GESTIONAR')")
    public PageResponse<Notification> inbox(
            @RequestParam(required = false) NotificationStatus estado,
            @RequestParam(required = false) NotificationTemplate plantilla,
            @PageableDefault(size = 25) Pageable pageable) {
        return service.inbox(estado, plantilla, pageable);
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAuthority('NOTIFICACIONES_GESTIONAR')")
    public long pending() {
        return service.pendingCount();
    }

    @PostMapping("/{id}/reintentar")
    @PreAuthorize("hasAuthority('NOTIFICACIONES_GESTIONAR')")
    public Notification retry(@PathVariable Long id) {
        return service.retry(id);
    }

    @PostMapping("/alquiler/{rentalId}/resumen")
    @PreAuthorize("hasAuthority('ALQUILERES_GESTIONAR')")
    public void sendSummary(@PathVariable Long rentalId) {
        service.sendRentalSummary(rentalId);
    }

    @PostMapping("/documento/{documentId}/reenviar")
    @PreAuthorize("hasAuthority('NOTIFICACIONES_GESTIONAR')")
    public Notification resendDocument(@PathVariable Long documentId,
                                       @RequestParam(required = false) String mensaje) {
        return service.resendDocument(documentId, mensaje);
    }

    @PostMapping("/reporte-diario")
    @PreAuthorize("hasAuthority('NOTIFICACIONES_GESTIONAR')")
    public Notification sendDailyReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.sendDailyReport(fecha == null ? LocalDate.now() : fecha);
    }

    @PostMapping("/seguimiento-morosos")
    @PreAuthorize("hasAuthority('NOTIFICACIONES_GESTIONAR')")
    public int overdueFollowUp(@RequestParam(defaultValue = "1") int diasMinimos) {
        return service.sendOverdueFollowUp(diasMinimos);
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAuthority('NOTIFICACIONES_GESTIONAR')")
    public Notification sendManual(@RequestBody ManualMessage request) {
        return service.sendManual(request.recipient(), request.message(),
                request.customerId(), request.rentalId());
    }

    public record ManualMessage(@NotBlank String recipient, @NotBlank String message,
                                Long customerId, Long rentalId) {
    }
}
