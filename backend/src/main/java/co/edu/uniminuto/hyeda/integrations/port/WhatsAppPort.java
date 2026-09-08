package co.edu.uniminuto.hyeda.integrations.port;

/**
 * Envio de mensajes por WhatsApp. La implementacion real usa la Cloud API de
 * Meta; en desarrollo se registra en el log para no gastar cuota ni molestar a
 * los clientes con mensajes de prueba.
 */
public interface WhatsAppPort {

    SendResult sendText(String recipient, String message);

    /**
     * @param documentPath ruta absoluta del PDF a adjuntar
     */
    SendResult sendDocument(String recipient, String caption, java.nio.file.Path documentPath,
                            String fileName);

    boolean enabled();

    record SendResult(boolean success, String providerResponse, String error) {

        public static SendResult ok(String response) {
            return new SendResult(true, response, null);
        }

        public static SendResult failed(String error) {
            return new SendResult(false, null, error);
        }
    }
}
