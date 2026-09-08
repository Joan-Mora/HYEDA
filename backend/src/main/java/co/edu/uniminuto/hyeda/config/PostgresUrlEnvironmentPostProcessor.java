package co.edu.uniminuto.hyeda.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

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
        if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://") && !raw.startsWith("jdbc:postgresql://")) {
            return;
        }

        try {
            RenderJdbcUrls.Parsed parsed = RenderJdbcUrls.parse(raw);
            Map<String, Object> map = new HashMap<>();
            map.put("spring.datasource.url", parsed.jdbcUrl());
            map.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

            if (StringUtils.hasText(parsed.username())
                    && !StringUtils.hasText(environment.getProperty("spring.datasource.username"))) {
                map.put("spring.datasource.username", parsed.username());
            }
            if (StringUtils.hasText(parsed.password())
                    && !StringUtils.hasText(environment.getProperty("spring.datasource.password"))) {
                map.put("spring.datasource.password", parsed.password());
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
