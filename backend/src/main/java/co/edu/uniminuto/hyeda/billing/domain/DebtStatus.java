package co.edu.uniminuto.hyeda.billing.domain;

public enum DebtStatus {

    ABIERTA,
    /** Recibio abonos pero aun queda saldo. */
    PARCIAL,
    PAGADA,
    /** Paso la fecha limite sin cancelarse. */
    VENCIDA,
    /** La empresa decide no cobrarla. */
    CONDONADA
}
