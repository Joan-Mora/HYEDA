package co.edu.uniminuto.hyeda.modules.alquileres.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExtensionRequest(@NotNull @Min(1) Integer extraDays) {}
