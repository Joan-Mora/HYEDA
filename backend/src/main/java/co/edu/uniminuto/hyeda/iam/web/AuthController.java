package co.edu.uniminuto.hyeda.iam.web;

import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.ChangePasswordRequest;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.LoginRequest;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.RefreshRequest;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.SessionResponse;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.UserProfile;
import co.edu.uniminuto.hyeda.iam.security.AuthenticatedUser;
import co.edu.uniminuto.hyeda.iam.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "app", "HYEDA");
    }

    @PostMapping("/login")
    public SessionResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public SessionResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        authService.logout(request == null ? null : request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserProfile me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.profile(user.getId());
    }

    @PostMapping("/cambiar-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
