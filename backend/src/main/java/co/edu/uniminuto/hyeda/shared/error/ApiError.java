package co.edu.uniminuto.hyeda.shared.error;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cuerpo uniforme de error para toda la API.
 *
 * @param fieldErrors errores por campo en validaciones; vacio en el resto de casos
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, Map.of());
    }

    public static ApiError validation(String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(LocalDateTime.now(), 400, "Datos invalidos", message, path, fieldErrors);
    }
}
