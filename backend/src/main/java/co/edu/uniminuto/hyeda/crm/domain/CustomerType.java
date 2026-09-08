package co.edu.uniminuto.hyeda.crm.domain;

/**
 * Clasificacion del cliente segun su historial con la empresa.
 * Determina si se le puede omitir el contrato de arrendamiento.
 */
public enum CustomerType {

    /** Primera vez o casi: siempre requiere contrato firmado. */
    NUEVO,
    /** Ya tiene historial, pero todavia no alcanza el nivel de confianza. */
    FRECUENTE,
    /** Historial limpio y suficiente: se le puede hacer solo remision. */
    CONFIANZA,
    /** Mora reiterada o incidentes: no se le deberia alquilar sin autorizacion. */
    RESTRINGIDO
}
