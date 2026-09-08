package co.edu.uniminuto.hyeda.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Render Free delivers {@code postgres://user:pass@host/db}; Spring needs {@code jdbc:postgresql://...}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PostgresUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = first(
                environment.getProperty("spring.datasource.url"),
                System.getenv("SPRING_DATASOURCE_URL"),
                System.getenv("DATABASE_URL"));
        if (!StringUtils.hasText(raw)) {
            return;
        }
        if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) {
            return;
        }

        try {
            String normalized = raw.startsWith("postgres://")
                    ? "postgresql://" + raw.substring("postgres://".length())
                    : raw;
            URI uri = URI.create(normalized);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "/hyeda" : uri.getPath();
            String jdbc = "jdbc:postgresql://" + host + ":" + port + path;
            if (StringUtils.hasText(uri.getQuery())) {
                jdbc = jdbc + "?" + uri.getQuery();
            }

            Map<String, Object> map = new HashMap<>();
            map.put("spring.datasource.url", jdbc);
            map.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

            if (uri.getUserInfo() != null) {
                String[] parts = uri.getUserInfo().split(":", 2);
                if (!StringUtils.hasText(environment.getProperty("spring.datasource.username"))) {
                    map.put("spring.datasource.username", parts[0]);
                }
                if (parts.length > 1 && !StringUtils.hasText(environment.getProperty("spring.datasource.password"))) {
                    map.put("spring.datasource.password", parts[1]);
                }
            }

            environment.getPropertySources().addFirst(new MapPropertySource("renderPostgresJdbc", map));
        } catch (IllegalArgumentException ignored) {
            // leave original URL; startup will fail with a clear datasource error
        }
    }

    private static String first(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return null;
    }
}
