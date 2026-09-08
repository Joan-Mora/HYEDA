package co.edu.uniminuto.hyeda.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds a Hikari DataSource that accepts Render-style {@code postgres://} / {@code postgresql://} URLs
 * (driver requires {@code jdbc:postgresql://host:port/db}).
 */
@Configuration
@Profile("postgres")
public class PostgresDataSourceConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.datasource.url")
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username:}") String username,
            @Value("${spring.datasource.password:}") String password) {
        Parsed parsed = parse(url);
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(parsed.jdbcUrl());
        ds.setUsername(StringUtils.hasText(username) ? username : parsed.username());
        ds.setPassword(StringUtils.hasText(password) ? password : parsed.password());
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(5);
        return ds;
    }

    static Parsed parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("spring.datasource.url is empty");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("jdbc:postgresql://")) {
            return new Parsed(trimmed, null, null);
        }
        if (!trimmed.startsWith("postgres://") && !trimmed.startsWith("postgresql://")) {
            // Assume already usable JDBC or let the driver fail clearly
            if (!trimmed.startsWith("jdbc:")) {
                trimmed = "jdbc:" + trimmed;
            }
            return new Parsed(trimmed, null, null);
        }

        String normalized = trimmed.startsWith("postgres://")
                ? "postgresql://" + trimmed.substring("postgres://".length())
                : trimmed;
        URI uri = URI.create(normalized);
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("Invalid Postgres URL (missing host)");
        }
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String path = uri.getPath() == null || uri.getPath().isBlank() ? "/hyeda" : uri.getPath();
        String jdbc = "jdbc:postgresql://" + host + ":" + port + path;
        if (StringUtils.hasText(uri.getQuery())) {
            jdbc = jdbc + "?" + uri.getQuery();
        }

        String user = null;
        String pass = null;
        if (uri.getUserInfo() != null) {
            String[] parts = uri.getUserInfo().split(":", 2);
            user = decode(parts[0]);
            if (parts.length > 1) {
                pass = decode(parts[1]);
            }
        }
        return new Parsed(jdbc, user, pass);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    record Parsed(String jdbcUrl, String username, String password) {}
}
