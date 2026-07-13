package com.tutorsys.controller;

import com.tutorsys.dto.ParentDto;
import com.tutorsys.service.ParentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parents")
public class ParentController {

    private final ParentService parentService;

    public ParentController(ParentService parentService) {
        this.parentService = parentService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ParentDto>> getAllParents() {
        return ResponseEntity.ok(parentService.getAllParents());
    }

    @GetMapping("/me")
    public ResponseEntity<ParentDto> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(parentService.getParentByUsername(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParentDto> getParentById(@PathVariable Long id, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        ParentDto dto = parentService.getParentById(id);
        if (!isAdmin && !dto.getUsername().equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParentDto> updateParent(@PathVariable Long id, @RequestBody ParentDto dto, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        ParentDto existing = parentService.getParentById(id);
        if (!isAdmin && !existing.getUsername().equals(authentication.getName())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(parentService.updateParent(id, dto));
    }
}
