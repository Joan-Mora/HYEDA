package co.edu.uniminuto.hyeda.iam.service;

import co.edu.uniminuto.hyeda.iam.domain.Permission;
import co.edu.uniminuto.hyeda.iam.domain.Role;
import co.edu.uniminuto.hyeda.iam.domain.UserAccount;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.CreateUserRequest;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.CreatedUserResponse;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.PermissionSummary;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.ResetPasswordResponse;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.RoleSummary;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.SaveRoleRequest;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.UpdateUserRequest;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.UserSummary;
import co.edu.uniminuto.hyeda.iam.repository.PermissionRepository;
import co.edu.uniminuto.hyeda.iam.repository.RefreshTokenRepository;
import co.edu.uniminuto.hyeda.iam.repository.RoleRepository;
import co.edu.uniminuto.hyeda.iam.repository.UserAccountRepository;
import co.edu.uniminuto.hyeda.shared.audit.AuditService;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import co.edu.uniminuto.hyeda.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final String ADMIN_ROLE = "ADMINISTRADOR";

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredentialGenerator credentialGenerator;
    private final AuditService auditService;
    private final CurrentUser currentUser;

    // ------------------------------------------------------------------ usuarios

    @Transactional(readOnly = true)
    public List<UserSummary> listUsers() {
        return userRepository.findAllByOrderByFullNameAsc().stream().map(this::toUserSummary).toList();
    }

    @Transactional
    public CreatedUserResponse createUser(CreateUserRequest request) {
        String username = (request.username() == null || request.username().isBlank())
                ? credentialGenerator.generateUsername(request.fullName(), userRepository::existsByUsernameIgnoreCase)
                : request.username().trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw BusinessException.of("El usuario '%s' ya existe", username);
        }

        boolean generated = request.password() == null || request.password().isBlank();
        String plainPassword = generated ? credentialGenerator.generatePassword() : request.password();
        if (plainPassword.length() < 8) {
            throw new BusinessException("La contrasena debe tener al menos 8 caracteres");
        }

        UserAccount account = UserAccount.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(plainPassword))
                .fullName(request.fullName().trim())
                .email(blankToNull(request.email()))
                .phone(blankToNull(request.phone()))
                .active(true)
                // Si el sistema genero la clave, el empleado debe cambiarla al entrar.
                .mustChangePassword(generated)
                .roles(resolveRoles(request.roleIds()))
                .build();

        UserAccount saved = userRepository.save(account);
        auditService.record("CREAR_USUARIO", "UserAccount", saved.getId(),
                "Usuario %s con roles %s".formatted(username, saved.roleNames()));

        return new CreatedUserResponse(toUserSummary(saved), plainPassword);
    }

    @Transactional
    public UserSummary updateUser(Long id, UpdateUserRequest request) {
        UserAccount account = findUser(id);

        if (Boolean.FALSE.equals(request.active())) {
            guardLastAdministrator(account, "desactivar");
        }

        account.setFullName(request.fullName().trim());
        account.setEmail(blankToNull(request.email()));
        account.setPhone(blankToNull(request.phone()));
        if (request.active() != null) {
            account.setActive(request.active());
            if (!request.active()) {
                refreshTokenRepository.revokeAllForUser(id);
            }
        }
        if (request.roleIds() != null) {
            Set<Role> roles = resolveRoles(request.roleIds());
            if (isLastAdministrator(account) && roles.stream().noneMatch(r -> ADMIN_ROLE.equals(r.getName()))) {
                throw new BusinessException(
                        "No puedes quitarle el rol ADMINISTRADOR al unico administrador activo");
            }
            account.setRoles(roles);
        }

        UserAccount saved = userRepository.save(account);
        auditService.record("EDITAR_USUARIO", "UserAccount", id, "Roles: " + saved.roleNames());
        return toUserSummary(saved);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(Long id) {
        UserAccount account = findUser(id);
        String plainPassword = credentialGenerator.generatePassword();
        account.setPasswordHash(passwordEncoder.encode(plainPassword));
        account.setMustChangePassword(true);
        userRepository.save(account);
        refreshTokenRepository.revokeAllForUser(id);

        auditService.record("RESET_PASSWORD", "UserAccount", id, "Contrasena regenerada");
        return new ResetPasswordResponse(account.getUsername(), plainPassword);
    }

    @Transactional
    public void deleteUser(Long id) {
        UserAccount account = findUser(id);
        if (account.getId().equals(currentUser.idOrNull())) {
            throw new BusinessException("No puedes eliminar tu propio usuario");
        }
        guardLastAdministrator(account, "eliminar");

        refreshTokenRepository.revokeAllForUser(id);
        userRepository.delete(account);
        auditService.record("ELIMINAR_USUARIO", "UserAccount", id, account.getUsername());
    }

    // --------------------------------------------------------------------- roles

    @Transactional(readOnly = true)
    public List<RoleSummary> listRoles() {
        List<UserAccount> users = userRepository.findAll();
        return roleRepository.findAllByOrderByNameAsc().stream()
                .map(role -> toRoleSummary(role, countUsers(users, role)))
                .toList();
    }

    @Transactional
    public RoleSummary createRole(SaveRoleRequest request) {
        String name = request.name().trim().toUpperCase().replace(' ', '_');
        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw BusinessException.of("Ya existe un rol llamado '%s'", name);
        }

        Role role = Role.builder()
                .name(name)
                .description(blankToNull(request.description()))
                .systemRole(false)
                .permissions(resolvePermissions(request.permissions()))
                .build();

        Role saved = roleRepository.save(role);
        auditService.record("CREAR_ROL", "Role", saved.getId(), name);
        return toRoleSummary(saved, 0);
    }

    @Transactional
    public RoleSummary updateRole(Long id, SaveRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Rol", id));

        if (Boolean.TRUE.equals(role.getSystemRole()) && ADMIN_ROLE.equals(role.getName())) {
            throw new BusinessException("El rol ADMINISTRADOR no se puede modificar");
        }
        if (!Boolean.TRUE.equals(role.getSystemRole())) {
            role.setName(request.name().trim().toUpperCase().replace(' ', '_'));
        }
        role.setDescription(blankToNull(request.description()));
        role.setPermissions(resolvePermissions(request.permissions()));

        Role saved = roleRepository.save(role);
        auditService.record("EDITAR_ROL", "Role", id, "Permisos: " + request.permissions());
        return toRoleSummary(saved, countUsers(userRepository.findAll(), saved));
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Rol", id));
        if (Boolean.TRUE.equals(role.getSystemRole())) {
            throw new BusinessException("Los roles del sistema no se pueden eliminar");
        }
        long assigned = countUsers(userRepository.findAll(), role);
        if (assigned > 0) {
            throw BusinessException.of("El rol tiene %d usuario(s) asignado(s); reasignalos primero", assigned);
        }
        roleRepository.delete(role);
        auditService.record("ELIMINAR_ROL", "Role", id, role.getName());
    }

    @Transactional(readOnly = true)
    public List<PermissionSummary> listPermissions() {
        return permissionRepository.findAllByOrderByModuleAscCodeAsc().stream()
                .map(p -> new PermissionSummary(p.getCode(), p.getModule(), p.getDescription()))
                .toList();
    }

    // ------------------------------------------------------------------ internos

    private UserAccount findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException("Alguno de los roles seleccionados no existe");
        }
        return new LinkedHashSet<>(roles);
    }

    private Set<Permission> resolvePermissions(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<Permission> permissions = permissionRepository.findByCodeIn(codes);
        if (permissions.size() != codes.size()) {
            throw new BusinessException("Alguno de los permisos seleccionados no existe");
        }
        return new LinkedHashSet<>(permissions);
    }

    /** Evita dejar el sistema sin ningun administrador que pueda entrar. */
    private void guardLastAdministrator(UserAccount account, String action) {
        if (isLastAdministrator(account)) {
            throw BusinessException.of("No puedes %s al unico administrador activo del sistema", action);
        }
    }

    private boolean isLastAdministrator(UserAccount account) {
        boolean isAdmin = account.roleNames().contains(ADMIN_ROLE);
        if (!isAdmin) {
            return false;
        }
        long activeAdmins = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .filter(u -> u.roleNames().contains(ADMIN_ROLE))
                .count();
        return activeAdmins <= 1;
    }

    private int countUsers(List<UserAccount> users, Role role) {
        return (int) users.stream().filter(u -> u.getRoles().stream()
                .anyMatch(r -> r.getId().equals(role.getId()))).count();
    }

    private UserSummary toUserSummary(UserAccount account) {
        return new UserSummary(account.getId(), account.getUsername(), account.getFullName(),
                account.getEmail(), account.getPhone(), Boolean.TRUE.equals(account.getActive()),
                Boolean.TRUE.equals(account.getMustChangePassword()), account.getLastLoginAt(),
                account.getCreatedAt(),
                account.getRoles().stream().map(r -> toRoleSummary(r, -1)).toList());
    }

    private RoleSummary toRoleSummary(Role role, int userCount) {
        return new RoleSummary(role.getId(), role.getName(), role.getDescription(),
                Boolean.TRUE.equals(role.getSystemRole()), userCount,
                role.getPermissions().stream().map(Permission::getCode).sorted().toList());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
