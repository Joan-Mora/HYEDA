package co.edu.uniminuto.hyeda.modules.clientes.dto;

import co.edu.uniminuto.hyeda.modules.clientes.entity.CustomerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomerRequest(
        @NotBlank String document,
        @NotBlank String name,
        String phone,
        String email,
        String address,
        @NotNull Boolean dataConsent,
        CustomerType customerType
) {}
