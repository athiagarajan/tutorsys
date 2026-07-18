package com.tutorsys.controller;

import com.tutorsys.dto.AuthRequest;
import com.tutorsys.dto.AuthResponse;
import com.tutorsys.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user login, credentials verification, and account registration")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates user credentials and retrieves a JWT authorization token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User successfully authenticated"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials supplied")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest loginRequest) {
        AuthResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register-parent")
    @Operation(summary = "Register Parent User", description = "Creates a new Parent login account and stores user details.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Parent registered successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request (e.g. username/email already taken)")
    })
    public ResponseEntity<String> registerParent(
            @Parameter(description = "Account username", example = "john_doe", required = true) @RequestParam String username,
            @Parameter(description = "Account password", example = "securepwd123", required = true) @RequestParam String password,
            @Parameter(description = "Account email address", example = "john.doe@example.com", required = true) @RequestParam String email,
            @Parameter(description = "Full name of the parent", example = "John Doe", required = true) @RequestParam String name,
            @Parameter(description = "Contact phone number", example = "+1234567890") @RequestParam(required = false) String phone,
            @Parameter(description = "Home address", example = "123 Main St, City") @RequestParam(required = false) String address,
            @Parameter(description = "Communication preference", example = "EMAIL") @RequestParam(required = false, defaultValue = "EMAIL") String preferredComm
    ) {
        try {
            authService.registerParentUser(username, password, email, name, phone, address, preferredComm);
            return ResponseEntity.ok("Parent registered successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
