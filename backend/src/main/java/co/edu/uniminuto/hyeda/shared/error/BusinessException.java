package co.edu.uniminuto.hyeda.shared.error;

/**
 * Regla de negocio incumplida. Se traduce a HTTP 409.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public static BusinessException of(String format, Object... args) {
        return new BusinessException(String.format(format, args));
    }
}
