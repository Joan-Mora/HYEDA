package co.edu.uniminuto.hyeda.rental.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados por los que pasa un alquiler, de principio a fin.
 *
 * <p>El alquiler que se registra al inicio no es la factura: es una estimacion.
 * La cifra definitiva se calcula cuando el material vuelve, sumando la mora o
 * restando los dias que el cliente no uso.
 */
public enum RentalStatus {

    /** Se esta armando en el wizard. El stock ya esta apartado. */
    BORRADOR,
    /** Datos completos y stock confirmado; falta emitir el documento. */
    RESERVADO,
    /** Contrato o remision generado y firmado. */
    DOCUMENTO_EMITIDO,
    /** El material salio hacia la obra. */
    EN_CURSO,
    /** El material regreso; falta liquidar. */
    DEVUELTO,
    /** Liquidacion calculada con mora, devolucion anticipada e incidencias. */
    FACTURACION_FINAL,
    /** Liquidado pero con saldo por cobrar. */
    PENDIENTE_PAGO,
    /** Pagado y cerrado. */
    CERRADO,
    CANCELADO;

    private static final Set<RentalStatus> TERMINAL = EnumSet.of(CERRADO, CANCELADO);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** El material esta fuera del patio. */
    public boolean isMaterialOut() {
        return this == EN_CURSO;
    }

    public Set<RentalStatus> allowedNext() {
        return switch (this) {
            case BORRADOR -> EnumSet.of(RESERVADO, CANCELADO);
            case RESERVADO -> EnumSet.of(DOCUMENTO_EMITIDO, BORRADOR, CANCELADO);
            case DOCUMENTO_EMITIDO -> EnumSet.of(EN_CURSO, CANCELADO);
            case EN_CURSO -> EnumSet.of(DEVUELTO);
            case DEVUELTO -> EnumSet.of(FACTURACION_FINAL);
            case FACTURACION_FINAL -> EnumSet.of(CERRADO, PENDIENTE_PAGO);
            case PENDIENTE_PAGO -> EnumSet.of(CERRADO);
            case CERRADO, CANCELADO -> EnumSet.noneOf(RentalStatus.class);
        };
    }

    public boolean canTransitionTo(RentalStatus target) {
        return allowedNext().contains(target);
    }
}
