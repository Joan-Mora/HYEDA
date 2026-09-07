package co.edu.uniminuto.hyeda.config;

import co.edu.uniminuto.hyeda.modules.alquileres.service.RentalService;
import co.edu.uniminuto.hyeda.modules.analytics.service.AnalyticsService;
import co.edu.uniminuto.hyeda.modules.finanzas.service.FinanceService;
import co.edu.uniminuto.hyeda.modules.integraciones.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final RentalService rentalService;
    private final FinanceService financeService;
    private final AnalyticsService analyticsService;
    private final NotificationService notificationService;

    public ScheduledJobs(RentalService rentalService,
                         FinanceService financeService,
                         AnalyticsService analyticsService,
                         NotificationService notificationService) {
        this.rentalService = rentalService;
        this.financeService = financeService;
        this.analyticsService = analyticsService;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = 300000)
    public void expireStockReservations() {
        rentalService.expireReservations();
    }

    @Scheduled(cron = "0 30 7 * * *")
    public void refreshDebts() {
        financeService.refreshOverdue();
    }

    @Scheduled(cron = "${hyeda.daily-report.cron:0 0 20 * * *}")
    public void dailyReport() {
        log.info("Generando reporte diario HYEDA");
        analyticsService.generateDailyPdfReport();
        notificationService.notifyAdmin("HYEDA: reporte diario generado. Revise panel de analiticas/documentos.");
    }
}
