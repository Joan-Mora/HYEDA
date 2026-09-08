package co.edu.uniminuto.hyeda.shared.config;

import co.edu.uniminuto.hyeda.catalog.service.StockService;
import co.edu.uniminuto.hyeda.finance.service.DebtService;
import co.edu.uniminuto.hyeda.notifications.service.NotificationOutbox;
import co.edu.uniminuto.hyeda.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Tareas programadas del sistema.
 *
 * <p>Cada una se protege con try/catch: un job que falla no debe impedir que los
 * demas corran en el siguiente ciclo, y el error queda en el log con contexto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobs {

    /** Dias de vencimiento a partir de los cuales se persigue una deuda. */
    private static final int FOLLOW_UP_FROM_DAYS = 3;

    private final StockService stockService;
    private final DebtService debtService;
    private final NotificationOutbox outbox;
    private final NotificationService notificationService;

    /**
     * Libera el stock de reservas que nadie confirmo. Sin esto, un alquiler
     * abandonado a medias dejaria el material bloqueado indefinidamente.
     */
    @Scheduled(cron = "${hyeda.jobs.expire-reservations-cron}", zone = "${hyeda.timezone}")
    public void expireReservations() {
        try {
            List<Long> affected = stockService.expireStaleReservations();
            if (!affected.isEmpty()) {
                log.info("Reservas vencidas liberadas en {} alquiler(es)", affected.size());
            }
        } catch (Exception e) {
            log.error("Fallo el job de expiracion de reservas", e);
        }
    }

    /** Despacha la bandeja de salida de WhatsApp. */
    @Scheduled(cron = "${hyeda.jobs.dispatch-notifications-cron}", zone = "${hyeda.timezone}")
    public void dispatchNotifications() {
        try {
            int sent = outbox.dispatchPending();
            if (sent > 0) {
                log.debug("Notificaciones enviadas: {}", sent);
            }
        } catch (Exception e) {
            log.error("Fallo el despacho de notificaciones", e);
        }
    }

    /** Recalcula la antiguedad de la cartera y avisa a quien esta por vencer. */
    @Scheduled(cron = "${hyeda.jobs.refresh-debts-cron}", zone = "${hyeda.timezone}")
    public void refreshDebts() {
        try {
            int updated = debtService.refreshAging();
            int reminders = notificationService.sendDueReminders();
            log.info("Cartera actualizada ({} deudas) y {} recordatorio(s) encolados", updated, reminders);
        } catch (Exception e) {
            log.error("Fallo el job de cartera", e);
        }
    }

    /** Reporte del dia a la administradora, al cerrar la jornada. */
    @Scheduled(cron = "${hyeda.jobs.daily-report-cron}", zone = "${hyeda.timezone}")
    public void dailyReport() {
        try {
            notificationService.sendDailyReport(LocalDate.now());
            log.info("Reporte diario encolado");
        } catch (Exception e) {
            log.error("Fallo el envio del reporte diario", e);
        }
    }

    /** Seguimiento semanal de clientes morosos. */
    @Scheduled(cron = "${hyeda.jobs.overdue-followup-cron}", zone = "${hyeda.timezone}")
    public void overdueFollowUp() {
        try {
            int queued = notificationService.sendOverdueFollowUp(FOLLOW_UP_FROM_DAYS);
            log.info("Seguimiento de morosos: {} mensaje(s) encolados", queued);
        } catch (Exception e) {
            log.error("Fallo el seguimiento de morosos", e);
        }
    }
}
