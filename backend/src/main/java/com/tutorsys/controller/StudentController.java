package com.tutorsys.controller;

import com.tutorsys.dto.StudentDto;
import com.tutorsys.dto.StudentRateDto;
import com.tutorsys.service.StudentService;
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
@RequestMapping("/api/students")
@Tag(name = "Student Management", description = "Endpoints for managing student profiles, registration, and subject-specific learning hourly rates")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @Operation(summary = "Get Students List", description = "Retrieves student profiles. Admins get all records; Parents get only their family students.")
    public ResponseEntity<List<StudentDto>> getStudents(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(studentService.getAllStudents());
        } else {
            return ResponseEntity.ok(studentService.getStudentsByParentUsername(authentication.getName()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Student by ID", description = "Retrieves a student profile by database ID. Parents are restricted to retrieving only their own children.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Student profile details returned"),
        @ApiResponse(responseCode = "403", description = "Access denied to requested profile")
    })
    public ResponseEntity<StudentDto> getStudentById(
            @Parameter(description = "Student database ID", example = "3", required = true) @PathVariable Long id, 
            Authentication authentication
    ) {
        StudentDto dto = studentService.getStudentById(id);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            // Check ownership
            if (!dto.getParentName().equalsIgnoreCase(authentication.getName()) && 
                !studentService.getStudentsByParentUsername(authentication.getName()).stream()
                        .anyMatch(s -> s.getId().equals(id))) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Student Profile", description = "Registers a new student profile and associates them with a parent database record. Admin only.")
    public ResponseEntity<StudentDto> createStudent(@RequestBody StudentDto dto) {
        return ResponseEntity.ok(studentService.createStudent(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Student Profile", description = "Updates basic information for a student. Admin only.")
    public ResponseEntity<StudentDto> updateStudent(
            @Parameter(description = "Student database ID", example = "3", required = true) @PathVariable Long id, 
            @RequestBody StudentDto dto
    ) {
        return ResponseEntity.ok(studentService.updateStudent(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Student Profile", description = "Removes a student and all associated calendars and sessions. Admin only.")
    @ApiResponses({
        @ApiResponse(responseCode = "244", description = "Student profile deleted successfully (No Content)")
    })
    public ResponseEntity<Void> deleteStudent(
            @Parameter(description = "Student database ID", example = "3", required = true) @PathVariable Long id
    ) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    // Student Rates endpoints
    @GetMapping("/{id}/rates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Student Subject Hourly Rates", description = "Retrieves customized subject billing rates set for a specific student. Admin only.")
    public ResponseEntity<List<StudentRateDto>> getRates(
            @Parameter(description = "Student database ID", example = "3", required = true) @PathVariable Long id
    ) {
        return ResponseEntity.ok(studentService.getStudentRates(id));
    }

    @PostMapping("/rates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add or Update Subject Hourly Rate", description = "Adds or updates the hourly billing rate for a student for a specific learning subject. Admin only.")
    public ResponseEntity<StudentRateDto> createOrUpdateRate(@RequestBody StudentRateDto dto) {
        return ResponseEntity.ok(studentService.addOrUpdateRate(dto));
    }
}
