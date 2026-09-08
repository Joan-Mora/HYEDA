package co.edu.uniminuto.hyeda.shared.security;

import co.edu.uniminuto.hyeda.iam.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Acceso al usuario autenticado desde la capa de servicio, sin tener que
 * arrastrar el principal por cada firma de metodo.
 */
@Component
public class CurrentUser {

    public Optional<AuthenticatedUser> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof AuthenticatedUser user ? Optional.of(user) : Optional.empty();
    }

    public Long idOrNull() {
        return get().map(AuthenticatedUser::getId).orElse(null);
    }

    /** Nombre para bitacora; "sistema" cuando la accion la ejecuta un job programado. */
    public String usernameOrSystem() {
        return get().map(AuthenticatedUser::getUsername).orElse("sistema");
    }

    public boolean hasPermission(String code) {
        return get().map(user -> user.getPermissions().contains(code)).orElse(false);
    }
}
