package co.edu.uniminuto.hyeda.integrations.port;

/**
 * Consulta de antecedentes por documento.
 *
 * <p>En Colombia no existe una API publica y gratuita de antecedentes para
 * terceros, asi que el modo por defecto es manual: el sistema abre la consulta
 * y el operador adjunta el certificado. Este puerto deja lista la conexion con
 * un proveedor comercial sin tocar el resto del sistema.
 */
public interface BackgroundCheckPort {

    Result consult(String document);

    String providerName();

    /**
     * @param resolved true si el proveedor ya entrego un veredicto; false si la
     *                 consulta queda pendiente de resolucion manual
     */
    record Result(boolean resolved, boolean hasRecords, String summary) {

        public static Result pendingManual() {
            return new Result(false, false, "Consulta abierta. Adjunta el certificado de antecedentes.");
        }

        public static Result submitted(String summary) {
            return new Result(false, false, summary);
        }
    }
}
