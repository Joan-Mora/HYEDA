package co.edu.uniminuto.hyeda.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hyeda")
public class HyedaProperties {
    private Jwt jwt = new Jwt();
    private String storagePath;
    private String assetsPath;
    private Admin admin = new Admin();
    private Company company = new Company();
    private Whatsapp whatsapp = new Whatsapp();
    private Payments payments = new Payments();
    private Maps maps = new Maps();
    private BackgroundCheck backgroundCheck = new BackgroundCheck();
    private int stockReservationMinutes = 30;
    private DailyReport dailyReport = new DailyReport();
    /** Comma-separated CORS origin patterns, e.g. https://hyeda-web.onrender.com,http://localhost:* */
    private String corsOrigins = "http://localhost:*,http://127.0.0.1:*";

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000;
    }

    @Data
    public static class Admin {
        private String username = "admin";
        private String password = "AdminHyeda2026!";
        private String fullName = "Administrador";
    }

    @Data
    public static class Company {
        private String name = "Herramientas y Equipos del Ariari";
        private String nit;
        private String city = "Granada, Meta";
    }

    @Data
    public static class Whatsapp {
        private boolean enabled;
        private String token;
        private String phoneNumberId;
        private String adminPhone;
    }

    @Data
    public static class Payments {
        private String provider = "mock";
        private Wompi wompi = new Wompi();

        @Data
        public static class Wompi {
            private String publicKey;
            private String privateKey;
            private String integrityKey;
        }
    }

    @Data
    public static class Maps {
        private String apiKey;
    }

    @Data
    public static class BackgroundCheck {
        private String provider = "manual";
        private String apiKey;
    }

    @Data
    public static class DailyReport {
        private String cron = "0 0 20 * * *";
    }
}
