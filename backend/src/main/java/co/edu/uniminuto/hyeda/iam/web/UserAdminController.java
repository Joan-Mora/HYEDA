package co.edu.uniminuto.hyeda.iam.web;

import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.CreateUserRequest;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.CreatedUserResponse;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.PermissionSummary;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.ResetPasswordResponse;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.RoleSummary;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.SaveRoleRequest;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.UpdateUserRequest;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.UserSummary;
import co.edu.uniminuto.hyeda.iam.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sistema")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USUARIOS_GESTIONAR')")
public class UserAdminController {

    private final UserAdminService service;

    @GetMapping("/usuarios")
    public List<UserSummary> listUsers() {
        return service.listUsers();
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedUserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return service.createUser(request);
    }

    @PutMapping("/usuarios/{id}")
    public UserSummary updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return service.updateUser(id, request);
    }

    @PostMapping("/usuarios/{id}/reset-password")
    public ResetPasswordResponse resetPassword(@PathVariable Long id) {
        return service.resetPassword(id);
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    public List<RoleSummary> listRoles() {
        return service.listRoles();
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    public RoleSummary createRole(@Valid @RequestBody SaveRoleRequest request) {
        return service.createRole(request);
    }

    @PutMapping("/roles/{id}")
    public RoleSummary updateRole(@PathVariable Long id, @Valid @RequestBody SaveRoleRequest request) {
        return service.updateRole(id, request);
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        service.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permisos")
    public List<PermissionSummary> listPermissions() {
        return service.listPermissions();
    }
}
