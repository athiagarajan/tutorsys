package com.tutorsys.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "System Health", description = "Endpoints for server status and cloud container liveness auditing checks")
public class HealthController {

    @GetMapping("/")
    @Operation(summary = "Check Server Health Status", description = "Simple endpoint returning status text indicating the server context has booted successfully.")
    public String health() {
        return "TutorSys API is online";
    }
}
