package co.edu.uniminuto.hyeda.integrations.port;

import java.math.BigDecimal;

/**
 * Puerto de cobro por QR.
 *
 * <p>La empresa hoy cobra en efectivo y por transferencia; el QR se resuelve con
 * un adaptador simulado que produce un enlace de pago local. Cuando se contrate
 * una pasarela real basta con publicar otro bean de esta interfaz.
 */
public interface PaymentGatewayPort {

    /**
     * @param reference referencia unica generada por HYEDA para el cobro
     * @return contenido que se codifica en el QR que ve el cliente
     */
    ChargeIntent createCharge(String reference, BigDecimal amount, String description);

    /** Nombre del proveedor, para dejarlo registrado en el pago. */
    String provider();

    record ChargeIntent(String reference, String qrContent, String checkoutUrl, String payload) {
    }
}
