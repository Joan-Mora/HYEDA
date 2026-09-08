package co.edu.uniminuto.hyeda.iam.domain;

/**
 * Codigos de permiso, para no repetir cadenas sueltas en las anotaciones
 * {@code @PreAuthorize} de los controladores.
 */
public final class Permissions {

    public static final String DASHBOARD_VER = "DASHBOARD_VER";
    public static final String INVENTARIO_VER = "INVENTARIO_VER";
    public static final String INVENTARIO_GESTIONAR = "INVENTARIO_GESTIONAR";
    public static final String CLIENTES_VER = "CLIENTES_VER";
    public static final String CLIENTES_GESTIONAR = "CLIENTES_GESTIONAR";
    public static final String ANTECEDENTES_CONSULTAR = "ANTECEDENTES_CONSULTAR";
    public static final String ALQUILERES_VER = "ALQUILERES_VER";
    public static final String ALQUILERES_GESTIONAR = "ALQUILERES_GESTIONAR";
    public static final String DOCUMENTOS_VER = "DOCUMENTOS_VER";
    public static final String PAGOS_REGISTRAR = "PAGOS_REGISTRAR";
    public static final String FINANZAS_VER = "FINANZAS_VER";
    public static final String FINANZAS_GESTIONAR = "FINANZAS_GESTIONAR";
    public static final String ANALITICAS_VER = "ANALITICAS_VER";
    public static final String IMPORTACION_EJECUTAR = "IMPORTACION_EJECUTAR";
    public static final String NOTIFICACIONES_GESTIONAR = "NOTIFICACIONES_GESTIONAR";
    public static final String USUARIOS_GESTIONAR = "USUARIOS_GESTIONAR";
    public static final String CONFIGURACION_GESTIONAR = "CONFIGURACION_GESTIONAR";

    private Permissions() {
    }
}
