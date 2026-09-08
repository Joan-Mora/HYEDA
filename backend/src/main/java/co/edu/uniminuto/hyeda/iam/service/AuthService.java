package co.edu.uniminuto.hyeda.iam.service;

import co.edu.uniminuto.hyeda.iam.domain.RefreshToken;
import co.edu.uniminuto.hyeda.iam.domain.UserAccount;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.ChangePasswordRequest;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.LoginRequest;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.SessionResponse;
import co.edu.uniminuto.hyeda.iam.dto.AuthDtos.UserProfile;
import co.edu.uniminuto.hyeda.iam.repository.RefreshTokenRepository;
import co.edu.uniminuto.hyeda.iam.repository.UserAccountRepository;
import co.edu.uniminuto.hyeda.iam.security.AuthenticatedUser;
import co.edu.uniminuto.hyeda.iam.security.JwtService;
import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import co.edu.uniminuto.hyeda.shared.error.BusinessException;
import co.edu.uniminuto.hyeda.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final HyedaProperties properties;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public SessionResponse login(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();

            UserAccount account = userRepository.findById(principal.getId()).orElseThrow();
            account.setLastLoginAt(LocalDateTime.now());
            userRepository.save(account);

            return issueSession(principal);
        } catch (DisabledException e) {
            throw new BusinessException("Tu usuario esta inactivo. Contacta al administrador.");
        } catch (BadCredentialsException e) {
            throw new BusinessException("Usuario o contrasena incorrectos");
        }
    }

    @Transactional
    public SessionResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new BusinessException("Sesion invalida. Inicia sesion de nuevo."));

        if (!stored.isUsable()) {
            throw new BusinessException("Tu sesion expiro. Inicia sesion de nuevo.");
        }

        // Rotacion: el token usado se invalida y se entrega uno nuevo.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueSession(new AuthenticatedUser(stored.getUser()));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional(readOnly = true)
    public UserProfile profile(Long userId) {
        UserAccount account = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", userId));
        return toProfile(account);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserAccount account = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", userId));

        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new BusinessException("La contrasena actual no coincide");
        }
        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new BusinessException("La nueva contrasena debe ser distinta de la actual");
        }

        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        account.setMustChangePassword(false);
        userRepository.save(account);

        // Cambiar la clave cierra todas las sesiones abiertas.
        refreshTokenRepository.revokeAllForUser(userId);
    }

    private SessionResponse issueSession(AuthenticatedUser principal) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = generateRefreshToken();

        UserAccount account = userRepository.getReferenceById(principal.getId());
        refreshTokenRepository.save(RefreshToken.builder()
                .user(account)
                .tokenHash(hash(refreshToken))
                .expiresAt(LocalDateTime.now().plusDays(properties.getSecurity().getRefreshTokenDays()))
                .build());

        return new SessionResponse(accessToken, refreshToken, jwtService.accessTokenSeconds(),
                new UserProfile(principal.getId(), principal.getUsername(), principal.getFullName(),
                        null, null, principal.isMustChangePassword(),
                        principal.getRoles(), principal.getPermissions()));
    }

    private UserProfile toProfile(UserAccount account) {
        return new UserProfile(account.getId(), account.getUsername(), account.getFullName(),
                account.getEmail(), account.getPhone(), Boolean.TRUE.equals(account.getMustChangePassword()),
                account.roleNames(), account.permissionCodes());
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
