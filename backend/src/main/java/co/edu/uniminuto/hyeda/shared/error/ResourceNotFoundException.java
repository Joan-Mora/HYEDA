package co.edu.uniminuto.hyeda.shared.error;

/**
 * Recurso inexistente. Se traduce a HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entity, Object id) {
        return new ResourceNotFoundException("%s %s no existe".formatted(entity, id));
    }
}
