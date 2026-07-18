package com.tutorsys.controller;

import com.tutorsys.dto.SubjectDto;
import com.tutorsys.entity.Subject;
import com.tutorsys.repository.SubjectRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subjects")
@Tag(name = "Subjects & Topics", description = "Endpoints for retrieving active tutoring subject items catalogs")
public class SubjectController {

    private final SubjectRepository subjectRepository;

    public SubjectController(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @GetMapping
    @Operation(summary = "Get Active Subjects Catalog", description = "Retrieves all subjects that are marked as active in the catalog (e.g. Mathematics, Science, English).")
    public ResponseEntity<List<SubjectDto>> getActiveSubjects() {
        List<SubjectDto> dtos = subjectRepository.findByActiveTrue().stream().map(sub -> {
            SubjectDto dto = new SubjectDto();
            dto.setId(sub.getId());
            dto.setName(sub.getName());
            dto.setDescription(sub.getDescription());
            dto.setActive(sub.isActive());
            return dto;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
}
