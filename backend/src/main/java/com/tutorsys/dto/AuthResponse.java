package com.tutorsys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "Response payload after successful authentication containing user profile and JWT authorization token")
public class AuthResponse {
    @Schema(description = "Bearer JWT token to access secured REST endpoints", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Authenticated user account ID", example = "1")
    private Long id;

    @Schema(description = "Authenticated username", example = "admin")
    private String username;

    @Schema(description = "User primary contact email address", example = "admin@tutorsys.com")
    private String email;

    @Schema(description = "Assigned user role (e.g. ROLE_ADMIN, ROLE_PARENT)", example = "ROLE_ADMIN")
    private String role;
}
