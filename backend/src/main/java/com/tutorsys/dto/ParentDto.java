package com.tutorsys.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ParentDto {
    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String preferredCommunication;
    private String notes;
    private List<StudentDto> students;
}
