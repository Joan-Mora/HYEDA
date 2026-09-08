package co.edu.uniminuto.hyeda.config;

import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Normalizes Render {@code postgres(ql)://} URLs to JDBC and adds SSL for public hosts.
 */
final class RenderJdbcUrls {

    private RenderJdbcUrls() {}

    record Parsed(String jdbcUrl, String username, String password) {}

    static Parsed parse(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("Datasource URL is empty");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("jdbc:postgresql://")) {
            return new Parsed(ensureSslForRenderExternal(trimmed), null, null);
        }
        if (!trimmed.startsWith("postgres://") && !trimmed.startsWith("postgresql://")) {
            if (!trimmed.startsWith("jdbc:")) {
                trimmed = "jdbc:" + trimmed;
            }
            return new Parsed(ensureSslForRenderExternal(trimmed), null, null);
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
        jdbc = ensureSslForRenderExternal(jdbc);

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

    static String ensureSslForRenderExternal(String jdbcUrl) {
        if (!StringUtils.hasText(jdbcUrl) || !jdbcUrl.contains("render.com")) {
            return jdbcUrl;
        }
        if (jdbcUrl.contains("sslmode=")) {
            return jdbcUrl;
        }
        return jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
