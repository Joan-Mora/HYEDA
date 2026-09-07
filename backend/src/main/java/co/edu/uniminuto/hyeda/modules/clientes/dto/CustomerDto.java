package co.edu.uniminuto.hyeda.modules.clientes.dto;

import co.edu.uniminuto.hyeda.modules.clientes.entity.CustomerType;

import java.math.BigDecimal;

public record CustomerDto(
        Long id,
        String document,
        String name,
        String phone,
        String email,
        String address,
        String idCopyPath,
        Boolean dataConsent,
        BigDecimal reputationScore,
        CustomerType customerType
) {}
