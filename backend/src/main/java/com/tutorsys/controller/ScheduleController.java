package com.tutorsys.controller;

import com.tutorsys.dto.ScheduleDto;
import com.tutorsys.service.ScheduleService;
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
@RequestMapping("/api/schedules")
@Tag(name = "Weekly Schedules", description = "Endpoints for configuring recurrent student weekly tutoring calendar time slot items")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    @Operation(summary = "Get Weekly Schedules List", description = "Retrieves configured schedules. Admins get all records; Parents get only their family student schedules.")
    public ResponseEntity<List<ScheduleDto>> getSchedules(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(scheduleService.getAllSchedules());
        } else {
            return ResponseEntity.ok(scheduleService.getSchedulesByParentUsername(authentication.getName()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Schedule Time Slot", description = "Configures a new recurring weekly tutoring schedule slot (e.g. Every Monday 4:00 PM). Admin only.")
    public ResponseEntity<ScheduleDto> createSchedule(@RequestBody ScheduleDto dto) {
        return ResponseEntity.ok(scheduleService.createSchedule(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Schedule Time Slot", description = "Updates settings for a recurring schedule slot. Admin only.")
    public ResponseEntity<ScheduleDto> updateSchedule(
            @Parameter(description = "Schedule database ID", example = "10", required = true) @PathVariable Long id, 
            @RequestBody ScheduleDto dto
    ) {
        return ResponseEntity.ok(scheduleService.updateSchedule(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Schedule Time Slot", description = "Removes a recurring schedule slot configuration. Admin only.")
    @ApiResponses({
        @ApiResponse(responseCode = "244", description = "Schedule deleted successfully (No Content)")
    })
    public ResponseEntity<Void> deleteSchedule(
            @Parameter(description = "Schedule database ID", example = "10", required = true) @PathVariable Long id
    ) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
