package co.edu.uniminuto.hyeda.modules.auth.controller;

import co.edu.uniminuto.hyeda.modules.auth.dto.LoginRequest;
import co.edu.uniminuto.hyeda.modules.auth.dto.LoginResponse;
import co.edu.uniminuto.hyeda.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "app", "HYEDA");
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me() {
        return ResponseEntity.ok().build();
    }
}
