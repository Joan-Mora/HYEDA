package co.edu.uniminuto.hyeda.analytics.web;

import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.Dashboard;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.HeldItem;
import co.edu.uniminuto.hyeda.analytics.dto.AnalyticsDtos.RangePreset;
import co.edu.uniminuto.hyeda.analytics.service.AnalyticsService;
import co.edu.uniminuto.hyeda.analytics.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@RestController
@RequestMapping("/api/analiticas")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final AnalyticsService analyticsService;
    private final ReportService reportService;

    @GetMapping("/tablero")
    @PreAuthorize("hasAuthority('ANALITICAS_VER')")
    public Dashboard dashboard(
            @RequestParam(required = false) RangePreset rango,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate[] range = resolveRange(rango, desde, hasta);
        return analyticsService.dashboard(range[0], range[1]);
    }

    @GetMapping("/material-en-calle")
    @PreAuthorize("hasAuthority('ANALITICAS_VER')")
    public List<HeldItem> heldItems() {
        return analyticsService.heldItems();
    }

    @GetMapping("/reporte.pdf")
    @PreAuthorize("hasAuthority('ANALITICAS_VER')")
    public ResponseEntity<byte[]> analyticsPdf(
            @RequestParam(required = false) RangePreset rango,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate[] range = resolveRange(rango, desde, hasta);
        byte[] pdf = reportService.analyticsPdf(range[0], range[1]);
        return file(pdf, MediaType.APPLICATION_PDF,
                "reporte-hyeda-%s-a-%s.pdf".formatted(range[0], range[1]));
    }

    @GetMapping("/reporte.xlsx")
    @PreAuthorize("hasAuthority('ANALITICAS_VER')")
    public ResponseEntity<byte[]> analyticsExcel(
            @RequestParam(required = false) RangePreset rango,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate[] range = resolveRange(rango, desde, hasta);
        byte[] excel = reportService.analyticsExcel(range[0], range[1]);
        return file(excel, XLSX, "reporte-hyeda-%s-a-%s.xlsx".formatted(range[0], range[1]));
    }

    @GetMapping("/reporte-operacion.pdf")
    @PreAuthorize("hasAuthority('ANALITICAS_VER')")
    public ResponseEntity<byte[]> operationPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate from = desde == null ? LocalDate.now() : desde;
        LocalDate to = hasta == null ? from : hasta;
        byte[] pdf = reportService.operationReportPdf(from, to, "Reporte de Operacion");
        return file(pdf, MediaType.APPLICATION_PDF, "operacion-%s-a-%s.pdf".formatted(from, to));
    }

    /**
     * Traduce el atajo del calendario a un rango concreto. Si vienen fechas
     * explicitas mandan ellas; el preset es solo comodidad de la pantalla.
     */
    private LocalDate[] resolveRange(RangePreset preset, LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return new LocalDate[]{from, to};
        }
        LocalDate reference = from != null ? from : (to != null ? to : LocalDate.now());
        RangePreset effective = preset == null ? RangePreset.MES : preset;

        return switch (effective) {
            case DIA -> new LocalDate[]{reference, reference};
            case SEMANA -> new LocalDate[]{
                    reference.with(java.time.DayOfWeek.MONDAY),
                    reference.with(java.time.DayOfWeek.SUNDAY)};
            case ANIO -> new LocalDate[]{
                    reference.withDayOfYear(1),
                    reference.with(TemporalAdjusters.lastDayOfYear())};
            case MES, PERSONALIZADO -> new LocalDate[]{
                    reference.withDayOfMonth(1),
                    reference.with(TemporalAdjusters.lastDayOfMonth())};
        };
    }

    private ResponseEntity<byte[]> file(byte[] content, MediaType type, String fileName) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(content);
    }
}
