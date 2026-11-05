package com.levelup.dto;

import com.levelup.dto.ApiDtos.UserResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record SignupRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
        public SignupRequest {
            if (email != null) email = email.trim();
        }
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {
        public LoginRequest {
            if (email != null) email = email.trim();
        }
    }

    public record LoginResponse(UserResponse user, String accessToken) {}
}
