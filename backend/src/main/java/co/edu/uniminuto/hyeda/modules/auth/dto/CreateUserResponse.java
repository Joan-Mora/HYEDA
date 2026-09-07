package co.edu.uniminuto.hyeda.modules.auth.dto;

public record CreateUserResponse(
        UserDto user,
        String generatedPassword
) {}
