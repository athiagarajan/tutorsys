package com.tutorsys.controller;

import com.tutorsys.dto.StudentDto;
import com.tutorsys.dto.StudentRateDto;
import com.tutorsys.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
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
    public ResponseEntity<StudentDto> getStudentById(@PathVariable Long id, Authentication authentication) {
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
    public ResponseEntity<StudentDto> createStudent(@RequestBody StudentDto dto) {
        return ResponseEntity.ok(studentService.createStudent(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentDto> updateStudent(@PathVariable Long id, @RequestBody StudentDto dto) {
        return ResponseEntity.ok(studentService.updateStudent(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    // Student Rates endpoints
    @GetMapping("/{id}/rates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StudentRateDto>> getRates(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentRates(id));
    }

    @PostMapping("/rates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StudentRateDto> createOrUpdateRate(@RequestBody StudentRateDto dto) {
        return ResponseEntity.ok(studentService.addOrUpdateRate(dto));
    }
}
