package com.tutorsys.service;

import com.tutorsys.dto.ParentDto;
import com.tutorsys.dto.StudentDto;
import com.tutorsys.dto.SubjectDto;
import com.tutorsys.entity.Parent;
import com.tutorsys.entity.Student;
import com.tutorsys.entity.Subject;
import com.tutorsys.repository.ParentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParentService {

    private final ParentRepository parentRepository;

    public ParentService(ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    @Transactional(readOnly = true)
    public List<ParentDto> getAllParents() {
        return parentRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ParentDto getParentById(Long id) {
        Parent parent = parentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found with id: " + id));
        return convertToDto(parent);
    }

    @Transactional(readOnly = true)
    public ParentDto getParentByUsername(String username) {
        Parent parent = parentRepository.findByUserUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found for username: " + username));
        return convertToDto(parent);
    }

    @Transactional
    public ParentDto updateParent(Long id, ParentDto dto) {
        Parent parent = parentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found with id: " + id));

        parent.setName(dto.getName());
        parent.setEmail(dto.getEmail());
        parent.setPhone(dto.getPhone());
        parent.setAddress(dto.getAddress());
        parent.setPreferredCommunication(dto.getPreferredCommunication());
        parent.setNotes(dto.getNotes());

        parent = parentRepository.save(parent);
        return convertToDto(parent);
    }

    public ParentDto convertToDto(Parent parent) {
        ParentDto dto = new ParentDto();
        dto.setId(parent.getId());
        if (parent.getUser() != null) {
            dto.setUserId(parent.getUser().getId());
            dto.setUsername(parent.getUser().getUsername());
        }
        dto.setName(parent.getName());
        dto.setEmail(parent.getEmail());
        dto.setPhone(parent.getPhone());
        dto.setAddress(parent.getAddress());
        dto.setPreferredCommunication(parent.getPreferredCommunication());
        dto.setNotes(parent.getNotes());
        return dto;
    }
}
