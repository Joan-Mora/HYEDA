package co.edu.uniminuto.hyeda.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank String username,
        String password,
        String fullName,
        String email,
        @NotEmpty Set<String> roles
) {}
