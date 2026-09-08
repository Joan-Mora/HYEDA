package co.edu.uniminuto.hyeda.rental.domain;

/**
 * Que documentacion respalda el alquiler.
 *
 * <p>El contrato de arrendamiento ya incluye el detalle de los productos, asi
 * que cuando hay contrato no se emite remision. La pareja remision + devolucion
 * es la alternativa liviana, reservada a clientes de confianza.
 */
public enum DocumentPolicy {

    CONTRATO,
    REMISION
}
