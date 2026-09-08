package co.edu.uniminuto.hyeda.integrations.web;

import co.edu.uniminuto.hyeda.integrations.port.PaymentGatewayPort;
import co.edu.uniminuto.hyeda.integrations.port.WhatsAppPort;
import co.edu.uniminuto.hyeda.integrations.service.MapsService;
import co.edu.uniminuto.hyeda.shared.company.CompanySettingsService;
import co.edu.uniminuto.hyeda.shared.config.HyedaProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integraciones")
public class IntegrationsController {

    private final HyedaProperties properties;
    private final WhatsAppPort whatsAppPort;
    private final PaymentGatewayPort paymentGatewayPort;
    private final MapsService mapsService;
    private final CompanySettingsService companySettingsService;

    public IntegrationsController(HyedaProperties properties,
                                  WhatsAppPort whatsAppPort,
                                  PaymentGatewayPort paymentGatewayPort,
                                  MapsService mapsService,
                                  CompanySettingsService companySettingsService) {
        this.properties = properties;
        this.whatsAppPort = whatsAppPort;
        this.paymentGatewayPort = paymentGatewayPort;
        this.mapsService = mapsService;
        this.companySettingsService = companySettingsService;
    }

    @GetMapping("/estado")
    @PreAuthorize("hasAuthority('CONFIGURACION_GESTIONAR')")
    public IntegrationStatus status() {
        HyedaProperties.Integrations integrations = properties.getIntegrations();
        return new IntegrationStatus(
                new ChannelStatus("whatsapp", whatsAppPort.enabled(),
                        integrations.getWhatsapp().getPhoneNumberId() != null
                                && !integrations.getWhatsapp().getPhoneNumberId().isBlank(),
                        whatsAppPort.enabled() ? "Cloud API" : "Simulado (log)"),
                new ChannelStatus("pagos", true,
                        !"mock".equalsIgnoreCase(integrations.getPayments().getProvider()),
                        paymentGatewayPort.getClass().getSimpleName()),
                new ChannelStatus("maps", mapsService.isConfigured(), mapsService.isConfigured(),
                        mapsService.isConfigured() ? "Google Maps" : "Enlaces de busqueda"),
                new ChannelStatus("antecedentes", true, true,
                        integrations.getBackgroundCheck().getProvider())
        );
    }

    @GetMapping("/maps-key")
    @PreAuthorize("isAuthenticated()")
    public MapsKey mapsKey() {
        return new MapsKey(mapsService.isConfigured(), mapsService.isConfigured() ? mapsService.apiKey() : null);
    }

    @PostMapping("/whatsapp/prueba")
    @PreAuthorize("hasAuthority('NOTIFICACIONES_GESTIONAR')")
    public WhatsAppPort.SendResult testWhatsApp() {
        String recipient = companySettingsService.get().getWhatsappAdmin();
        if (recipient == null || recipient.isBlank()) {
            recipient = properties.getAdmin().getPhone();
        }
        if (recipient == null || recipient.isBlank()) {
            recipient = "573000000000";
        }
        return whatsAppPort.sendText(recipient, "HYEDA: mensaje de prueba del patio. Si lo lees, WhatsApp esta listo.");
    }

    public record IntegrationStatus(ChannelStatus whatsapp, ChannelStatus pagos, ChannelStatus maps,
                                    ChannelStatus antecedentes) {
    }

    public record ChannelStatus(String id, boolean enabled, boolean configured, String provider) {
    }

    public record MapsKey(boolean configured, String apiKey) {
    }
}
