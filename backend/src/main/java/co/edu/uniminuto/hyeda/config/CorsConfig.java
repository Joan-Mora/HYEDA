package co.edu.uniminuto.hyeda.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private final HyedaProperties properties;

    public CorsConfig(HyedaProperties properties) {
        this.properties = properties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        String raw = properties.getCorsOrigins() == null ? "" : properties.getCorsOrigins();
        List<String> origins = Arrays.stream(raw.split(","))
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .toList();
        if (origins.isEmpty()) {
            origins = List.of("http://localhost:*", "http://127.0.0.1:*");
        }
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
