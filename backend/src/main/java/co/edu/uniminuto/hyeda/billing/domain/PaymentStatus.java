package co.edu.uniminuto.hyeda.billing.domain;

public enum PaymentStatus {

    /** QR emitido, a la espera de que el cliente pague. */
    PENDIENTE,
    CONFIRMADO,
    FALLIDO,
    ANULADO
}
