package com.tutorsys.controller;

import com.tutorsys.dto.ParentDto;
import com.tutorsys.service.ParentService;
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
@RequestMapping("/api/parents")
@Tag(name = "Parent Management", description = "Endpoints for retrieving, listing, and updating parent profiles")
public class ParentController {

    private final ParentService parentService;

    public ParentController(ParentService parentService) {
        this.parentService = parentService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get All Parents", description = "Lists profiles for all registered parents in the system. Admin only.")
    public ResponseEntity<List<ParentDto>> getAllParents() {
        return ResponseEntity.ok(parentService.getAllParents());
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current Parent Profile", description = "Retrieves profile and contact details for the currently logged-in parent user.")
    public ResponseEntity<ParentDto> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(parentService.getParentByUsername(authentication.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Parent by ID", description = "Retrieves a parent profile by database ID. Parents are restricted to retrieving only their own profiles.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Parent profile details returned"),
        @ApiResponse(responseCode = "403", description = "Access denied to requested profile")
    })
    public ResponseEntity<ParentDto> getParentById(
            @Parameter(description = "Parent database ID", example = "5", required = true) @PathVariable Long id, 
            Authentication authentication
    ) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        ParentDto dto = parentService.getParentById(id);
        if (!isAdmin && !dto.getUsername().equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Parent Profile", description = "Updates profile contact configurations. Parents are restricted to updating only their own profiles.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Parent profile updated successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied to update requested profile")
    })
    public ResponseEntity<ParentDto> updateParent(
            @Parameter(description = "Parent database ID", example = "5", required = true) @PathVariable Long id, 
            @RequestBody ParentDto dto, 
            Authentication authentication
    ) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        ParentDto existing = parentService.getParentById(id);
        if (!isAdmin && !existing.getUsername().equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(parentService.updateParent(id, dto));
    }
}
