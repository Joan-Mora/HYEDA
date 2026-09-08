package co.edu.uniminuto.hyeda.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Contratos de entrada y salida del modulo de identidad.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "El usuario es obligatorio") String username,
            @NotBlank(message = "La contrasena es obligatoria") String password
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record SessionResponse(
            String accessToken,
            String refreshToken,
            long expiresInSeconds,
            UserProfile user
    ) {
    }

    public record UserProfile(
            Long id,
            String username,
            String fullName,
            String email,
            String phone,
            boolean mustChangePassword,
            Set<String> roles,
            Set<String> permissions
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "La contrasena actual es obligatoria") String currentPassword,
            @NotBlank @Size(min = 8, message = "La nueva contrasena debe tener al menos 8 caracteres")
            String newPassword
    ) {
    }

    public record CreateUserRequest(
            @NotBlank(message = "El nombre completo es obligatorio") String fullName,
            String username,
            String email,
            String phone,
            /** Si viene vacia, el sistema genera una contrasena segura y la devuelve una sola vez. */
            String password,
            Set<Long> roleIds
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank(message = "El nombre completo es obligatorio") String fullName,
            String email,
            String phone,
            Boolean active,
            Set<Long> roleIds
    ) {
    }

    /**
     * Respuesta de alta de usuario. {@code generatedPassword} solo viaja en esta
     * respuesta para que el administrador pueda copiarla y entregarla; despues
     * ya no es recuperable.
     */
    public record CreatedUserResponse(UserSummary user, String generatedPassword) {
    }

    public record UserSummary(
            Long id,
            String username,
            String fullName,
            String email,
            String phone,
            boolean active,
            boolean mustChangePassword,
            LocalDateTime lastLoginAt,
            LocalDateTime createdAt,
            List<RoleSummary> roles
    ) {
    }

    public record RoleSummary(
            Long id,
            String name,
            String description,
            boolean systemRole,
            int userCount,
            List<String> permissions
    ) {
    }

    public record SaveRoleRequest(
            @NotBlank(message = "El nombre del rol es obligatorio") String name,
            String description,
            Set<String> permissions
    ) {
    }

    public record PermissionSummary(String code, String module, String description) {
    }

    public record ResetPasswordResponse(String username, String generatedPassword) {
    }
}
