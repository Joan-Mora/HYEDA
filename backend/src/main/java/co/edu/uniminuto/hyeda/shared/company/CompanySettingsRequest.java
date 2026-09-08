package co.edu.uniminuto.hyeda.shared.company;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CompanySettingsRequest(
        @NotBlank(message = "La razon social es obligatoria") String legalName,
        @NotBlank(message = "El nombre comercial es obligatorio") String tradeName,
        @NotBlank(message = "El NIT es obligatorio") String nit,
        String ownerName,
        String taxRegime,
        String phone,
        @Email(message = "Correo invalido") String email,
        String address,
        String city,
        String whatsappAdmin,
        @Min(value = 0, message = "Los dias de gracia no pueden ser negativos") Integer lateGraceDays
) {
}
