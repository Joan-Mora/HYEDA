package co.edu.uniminuto.hyeda.modules.auth.dto;

import java.util.Set;

public record RoleDto(
        Long id,
        String name,
        String description,
        Set<String> permissions
) {}
