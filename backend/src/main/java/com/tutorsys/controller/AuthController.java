package com.tutorsys.controller;

import com.tutorsys.dto.AuthRequest;
import com.tutorsys.dto.AuthResponse;
import com.tutorsys.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest loginRequest) {
        AuthResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register-parent")
    public ResponseEntity<String> registerParent(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false, defaultValue = "EMAIL") String preferredComm
    ) {
        try {
            authService.registerParentUser(username, password, email, name, phone, address, preferredComm);
            return ResponseEntity.ok("Parent registered successfully!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
