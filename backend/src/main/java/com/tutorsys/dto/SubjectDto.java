package com.tutorsys.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubjectDto {
    private Long id;
    private String name;
    private String description;
    private boolean active;
}
