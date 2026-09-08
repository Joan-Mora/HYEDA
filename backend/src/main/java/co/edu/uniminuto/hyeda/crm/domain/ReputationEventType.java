package co.edu.uniminuto.hyeda.crm.domain;

import java.math.BigDecimal;

/**
 * Hechos que mueven la reputacion de un cliente, con su peso por defecto.
 * Los negativos pesan mas que los positivos: la confianza cuesta ganarla.
 */
public enum ReputationEventType {

    DEVOLUCION_A_TIEMPO(new BigDecimal("3")),
    PAGO_COMPLETO(new BigDecimal("2")),
    ABONO(new BigDecimal("1")),
    MORA(new BigDecimal("-6")),
    DANIO(new BigDecimal("-8")),
    FALTANTE(new BigDecimal("-12")),
    MANUAL(BigDecimal.ZERO);

    private final BigDecimal defaultDelta;

    ReputationEventType(BigDecimal defaultDelta) {
        this.defaultDelta = defaultDelta;
    }

    public BigDecimal defaultDelta() {
        return defaultDelta;
    }
}
