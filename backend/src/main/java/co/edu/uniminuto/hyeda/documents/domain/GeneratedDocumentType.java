package co.edu.uniminuto.hyeda.documents.domain;

public enum GeneratedDocumentType {

    /** Contrato de arrendamiento; incluye el detalle de equipos, asi que reemplaza a la remision. */
    CONTRATO("CTR", "Contrato de Arrendamiento", "contrato"),
    /** Salida de material sin contrato, para clientes de confianza. */
    REMISION("REM", "Remision de Equipos", "remision"),
    /** Cierre de la remision: constancia de lo que regreso al patio. */
    DEVOLUCION("DEV", "Acta de Devolucion", "devolucion"),
    FACTURA("FAC", "Cuenta de Cobro", "factura"),
    REPORTE("REP", "Reporte", "reporte-diario");

    private final String prefix;
    private final String title;
    private final String templateName;

    GeneratedDocumentType(String prefix, String title, String templateName) {
        this.prefix = prefix;
        this.title = title;
        this.templateName = templateName;
    }

    public String prefix() {
        return prefix;
    }

    public String title() {
        return title;
    }

    public String templatePath() {
        return "documents/" + templateName;
    }
}
