package com.tutorsys.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class StudentDto {
    private Long id;
    private Long parentId;
    private String parentName;
    private String firstName;
    private String lastName;
    private String preferredName;
    private String grade;
    private String school;
    private LocalDate dateJoined;
    private String status;
    private String notes;
    private Set<SubjectDto> subjects;
}
