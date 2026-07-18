package com.tutorsys.controller;

import com.tutorsys.dto.SessionDto;
import com.tutorsys.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@Tag(name = "Tutoring Sessions", description = "Endpoints for logging and tracking completed student tutoring session events")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    @Operation(summary = "Get Completed Sessions List", description = "Retrieves logged tutoring sessions. Admins get all records; Parents get only their family tutoring history.")
    public ResponseEntity<List<SessionDto>> getSessions(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(sessionService.getAllSessions());
        } else {
            return ResponseEntity.ok(sessionService.getSessionsByParentUsername(authentication.getName()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Log Tutoring Session", description = "Logs a completed student-tutor tutoring session session, specifying duration and status. Admin only.")
    public ResponseEntity<SessionDto> createSession(@RequestBody SessionDto dto) {
        return ResponseEntity.ok(sessionService.createSession(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Logged Session", description = "Updates settings for a completed tutoring session event. Admin only.")
    public ResponseEntity<SessionDto> updateSession(
            @Parameter(description = "Session database ID", example = "20", required = true) @PathVariable Long id, 
            @RequestBody SessionDto dto
    ) {
        return ResponseEntity.ok(sessionService.updateSession(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Logged Session", description = "Removes a logged tutoring session event. Admin only.")
    @ApiResponses({
        @ApiResponse(responseCode = "244", description = "Session deleted successfully (No Content)")
    })
    public ResponseEntity<Void> deleteSession(
            @Parameter(description = "Session database ID", example = "20", required = true) @PathVariable Long id
    ) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
