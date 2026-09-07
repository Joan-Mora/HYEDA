package co.edu.uniminuto.hyeda.modules.auth.service;

import co.edu.uniminuto.hyeda.modules.auth.dto.*;
import co.edu.uniminuto.hyeda.modules.auth.entity.Permission;
import co.edu.uniminuto.hyeda.modules.auth.entity.Role;
import co.edu.uniminuto.hyeda.modules.auth.entity.UserAccount;
import co.edu.uniminuto.hyeda.modules.auth.repository.PermissionRepository;
import co.edu.uniminuto.hyeda.modules.auth.repository.RoleRepository;
import co.edu.uniminuto.hyeda.modules.auth.repository.UserAccountRepository;
import co.edu.uniminuto.hyeda.shared.exception.BusinessException;
import co.edu.uniminuto.hyeda.shared.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(UserAccountRepository userAccountRepository,
                            RoleRepository roleRepository,
                            PermissionRepository permissionRepository,
                            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDto> listUsers() {
        return userAccountRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new BusinessException("El usuario ya existe");
        }
        String rawPassword = (request.password() == null || request.password().isBlank())
                ? generatePassword()
                : request.password();
        Set<Role> roles = request.roles().stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + name)))
                .collect(Collectors.toSet());
        UserAccount user = UserAccount.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(request.fullName())
                .email(request.email())
                .active(true)
                .roles(roles)
                .build();
        userAccountRepository.save(user);
        return new CreateUserResponse(toDto(user), rawPassword);
    }

    public List<RoleDto> listRoles() {
        return roleRepository.findAll().stream().map(this::toRoleDto).toList();
    }

    @Transactional
    public RoleDto createRole(String name, String description, Set<String> permissionCodes) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new BusinessException("El rol ya existe");
        }
        Set<Permission> permissions = new HashSet<>();
        if (permissionCodes != null) {
            for (String code : permissionCodes) {
                permissions.add(permissionRepository.findByCode(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado: " + code)));
            }
        }
        Role role = Role.builder().name(name).description(description).permissions(permissions).build();
        return toRoleDto(roleRepository.save(role));
    }

    public List<PermissionDto> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionDto(p.getId(), p.getCode(), p.getDescription()))
                .toList();
    }

    private UserDto toDto(UserAccount u) {
        return new UserDto(
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getEmail(),
                u.getActive(),
                u.getRoles().stream().map(r -> r.getName()).toList()
        );
    }

    private RoleDto toRoleDto(Role r) {
        return new RoleDto(
                r.getId(),
                r.getName(),
                r.getDescription(),
                r.getPermissions().stream().map(p -> p.getCode()).collect(Collectors.toSet())
        );
    }

    private String generatePassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
