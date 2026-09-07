package co.edu.uniminuto.hyeda.modules.auth.dto;

import java.util.List;

public record UserDto(
        Long id,
        String username,
        String fullName,
        String email,
        Boolean active,
        List<String> roles
) {}
