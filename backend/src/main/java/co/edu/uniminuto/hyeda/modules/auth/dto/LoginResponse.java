package co.edu.uniminuto.hyeda.modules.auth.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String username,
        String fullName,
        List<String> roles,
        List<String> permissions
) {}
