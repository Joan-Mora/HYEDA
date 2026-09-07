package co.edu.uniminuto.hyeda.modules.auth.service;

import co.edu.uniminuto.hyeda.config.JwtService;
import co.edu.uniminuto.hyeda.modules.auth.dto.LoginRequest;
import co.edu.uniminuto.hyeda.modules.auth.dto.LoginResponse;
import co.edu.uniminuto.hyeda.modules.auth.entity.UserAccount;
import co.edu.uniminuto.hyeda.modules.auth.repository.UserAccountRepository;
import co.edu.uniminuto.hyeda.shared.exception.ResourceNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserAccountRepository userAccountRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserAccount user = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        List<String> roles = user.getRoles().stream().map(r -> r.getName()).toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getCode())
                .distinct()
                .toList();
        String token = jwtService.generateToken(user.getUsername(), roles, permissions);
        return new LoginResponse(token, user.getUsername(), user.getFullName(), roles, permissions);
    }
}
