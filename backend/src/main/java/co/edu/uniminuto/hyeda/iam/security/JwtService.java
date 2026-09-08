package co.edu.uniminuto.hyeda.iam.security;

import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class JwtService {

    private final HyedaProperties properties;

    public JwtService(HyedaProperties properties) {
        this.properties = properties;
    }

    private SecretKey key() {
        String secret = properties.getSecurity().getJwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "hyeda.security.jwt-secret debe tener al menos 32 bytes. Configura HYEDA_JWT_SECRET.");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getSecurity().getAccessTokenMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("name", user.getFullName())
                .claim("roles", List.copyOf(user.getRoles()))
                .claim("permissions", List.copyOf(user.getPermissions()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key())
                .compact();
    }

    public Optional<String> extractUsername(String token) {
        return parse(token).map(Claims::getSubject);
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT invalido: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public long accessTokenSeconds() {
        return properties.getSecurity().getAccessTokenMinutes() * 60L;
    }
}
