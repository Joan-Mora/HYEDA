package co.edu.uniminuto.hyeda.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

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
        RenderJdbcUrls.Parsed parsed = RenderJdbcUrls.parse(url);
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(parsed.jdbcUrl());
        ds.setUsername(StringUtils.hasText(username) ? username : parsed.username());
        ds.setPassword(StringUtils.hasText(password) ? password : parsed.password());
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(5);
        return ds;
    }
}
