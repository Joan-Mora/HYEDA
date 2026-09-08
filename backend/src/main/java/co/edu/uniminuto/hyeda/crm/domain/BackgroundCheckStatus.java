package co.edu.uniminuto.hyeda.crm.domain;

public enum BackgroundCheckStatus {

    /** Consulta abierta: falta adjuntar el certificado o registrar el resultado. */
    PENDIENTE_MANUAL,
    /** Enviada a un proveedor externo, a la espera de respuesta. */
    ENVIADO,
    SIN_ANTECEDENTES,
    CON_ANTECEDENTES,
    /** El operador decide no alquilarle a esta persona. */
    RECHAZADO
}
