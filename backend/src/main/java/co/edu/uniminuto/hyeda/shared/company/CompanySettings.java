package co.edu.uniminuto.hyeda.shared.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parametros de la empresa. Fila unica (id = 1); alimenta el encabezado de
 * todos los documentos PDF y el destinatario del reporte diario.
 */
@Entity
@Table(name = "company_settings")
@Getter
@Setter
@NoArgsConstructor
public class CompanySettings {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id = SINGLETON_ID;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "trade_name", nullable = false)
    private String tradeName;

    @Column(nullable = false)
    private String nit;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "tax_regime")
    private String taxRegime;

    private String phone;

    private String email;

    private String address;

    private String city;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "whatsapp_admin")
    private String whatsappAdmin;

    /** Dias de gracia antes de empezar a cobrar mora. */
    @Column(name = "late_grace_days", nullable = false)
    private Integer lateGraceDays = 0;

    @Column(name = "daily_report_cron", nullable = false)
    private String dailyReportCron = "0 0 20 * * *";
}
