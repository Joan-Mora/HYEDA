package co.edu.uniminuto.hyeda.catalog.domain;

/**
 * Razon por la que cambio el stock de un producto.
 */
public enum StockMovementType {

    /** Carga inicial o masiva desde CSV/Excel. */
    IMPORTACION,
    /** Correccion manual del inventario. */
    AJUSTE,
    /** Se aparta al abrir un alquiler. */
    RESERVA,
    /** Se devuelve al patio porque la reserva expiro o el alquiler se cancelo. */
    LIBERACION,
    /** Sale fisicamente hacia la obra. */
    DESPACHO,
    /** Vuelve del cliente en buen estado. */
    RETORNO,
    /** No vuelve o vuelve inservible: se descuenta del total. */
    BAJA_POR_DANIO
}
