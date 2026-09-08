package co.edu.uniminuto.hyeda.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Configuracion de la aplicacion bajo el prefijo {@code hyeda}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "hyeda")
public class HyedaProperties {

    private String timezone = "America/Bogota";
    private String storagePath = "./storage";
    /** Base publica del frontend; se usa para armar enlaces de verificacion de documentos. */
    private String publicBaseUrl = "http://localhost:5173";

    private Cors cors = new Cors();
    private Security security = new Security();
    private Admin admin = new Admin();
    private Rental rental = new Rental();
    private Jobs jobs = new Jobs();
    private Integrations integrations = new Integrations();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOriginPatterns = List.of("http://localhost:*");
    }

    @Getter
    @Setter
    public static class Security {
        private String jwtSecret;
        private int accessTokenMinutes = 120;
        private int refreshTokenDays = 14;
    }

    @Getter
    @Setter
    public static class Admin {
        private String username = "admin";
        private String password = "AdminHyeda2026!";
        private String fullName = "Administrador";
        private String phone;
    }

    @Getter
    @Setter
    public static class Rental {
        private int reservationMinutes = 30;
        private int trustedMinRentals = 3;
        private BigDecimal trustedMinReputation = new BigDecimal("75");
    }

    @Getter
    @Setter
    public static class Jobs {
        private String expireReservationsCron = "0 */5 * * * *";
        private String refreshDebtsCron = "0 30 7 * * *";
        private String dailyReportCron = "0 0 20 * * *";
        private String dispatchNotificationsCron = "0 * * * * *";
        private String overdueFollowupCron = "0 0 9 * * MON";
    }

    @Getter
    @Setter
    public static class Integrations {
        private WhatsApp whatsapp = new WhatsApp();
        private Payments payments = new Payments();
        private Maps maps = new Maps();
        private BackgroundCheck backgroundCheck = new BackgroundCheck();
    }

    @Getter
    @Setter
    public static class WhatsApp {
        private boolean enabled = false;
        private String token;
        private String phoneNumberId;
        private String apiVersion = "v21.0";
    }

    @Getter
    @Setter
    public static class Payments {
        private String provider = "mock";
        private String currency = "COP";
        private Wompi wompi = new Wompi();
    }

    @Getter
    @Setter
    public static class Wompi {
        private String publicKey;
        private String privateKey;
        private String integrityKey;
    }

    @Getter
    @Setter
    public static class Maps {
        private String apiKey;
    }

    @Getter
    @Setter
    public static class BackgroundCheck {
        private String provider = "manual";
        private String apiKey;
        private String baseUrl;
    }
}
