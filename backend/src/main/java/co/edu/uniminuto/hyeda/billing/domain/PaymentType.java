package co.edu.uniminuto.hyeda.billing.domain;

public enum PaymentType {

    EFECTIVO,
    /** Pago por codigo QR generado en el mostrador. */
    QR,
    TRANSFERENCIA,
    /** Salida de dinero hacia el cliente por haber pagado de mas. */
    REEMBOLSO,
    /** Correccion contable, no implica movimiento de caja. */
    AJUSTE;

    public boolean isOutgoing() {
        return this == REEMBOLSO;
    }
}
