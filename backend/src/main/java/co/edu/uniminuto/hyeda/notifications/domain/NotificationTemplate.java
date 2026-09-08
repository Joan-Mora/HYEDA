package co.edu.uniminuto.hyeda.notifications.domain;

/**
 * Catalogo de mensajes que envia el sistema. El codigo queda guardado en cada
 * notificacion para poder filtrar el historial y medir que se esta enviando.
 */
public enum NotificationTemplate {

    /** Resumen del alquiler recien abierto, al cliente. */
    RESUMEN_ALQUILER("Resumen de alquiler"),
    /** Aviso de que el plazo vence pronto. */
    RECORDATORIO_VENCIMIENTO("Recordatorio de vencimiento"),
    /** Cuenta de cobro adjunta al cerrar el alquiler. */
    FACTURA_CIERRE("Cuenta de cobro"),
    /** Cobro de cartera al cliente moroso. */
    COBRO_DEUDA("Cobro de deuda"),
    /** Reporte diario en PDF a la administradora. */
    REPORTE_DIARIO("Reporte diario"),
    /** Listado de morosos a la administradora. */
    SEGUIMIENTO_MOROSOS("Seguimiento de morosos"),
    MANUAL("Mensaje manual");

    private final String label;

    NotificationTemplate(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
