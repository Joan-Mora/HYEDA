package co.edu.uniminuto.hyeda.modules.auth.controller;

import co.edu.uniminuto.hyeda.modules.auth.dto.*;
import co.edu.uniminuto.hyeda.modules.auth.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final UserAdminService userAdminService;

    public AdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping("/users")
    public List<UserDto> users() {
        return userAdminService.listUsers();
    }

    @PostMapping("/users")
    public CreateUserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userAdminService.createUser(request);
    }

    @GetMapping("/roles")
    public List<RoleDto> roles() {
        return userAdminService.listRoles();
    }

    @PostMapping("/roles")
    public RoleDto createRole(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        @SuppressWarnings("unchecked")
        Set<String> permissions = body.get("permissions") == null
                ? Set.of()
                : Set.copyOf((List<String>) body.get("permissions"));
        return userAdminService.createRole(name, description, permissions);
    }

    @GetMapping("/permissions")
    public List<PermissionDto> permissions() {
        return userAdminService.listPermissions();
    }
}
