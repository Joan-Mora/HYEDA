package co.edu.uniminuto.hyeda.modules.alquileres.dto;

import java.time.LocalDate;

public record ReturnRentalRequest(
        LocalDate actualReturnDate,
        String incidentReason,
        String incidentDescription
) {}
