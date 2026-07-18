package com.tutorsys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request payload for user login authentication")
public class AuthRequest {
    @NotBlank(message = "Username is required")
    @Schema(description = "Registered username", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account secret password", example = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
