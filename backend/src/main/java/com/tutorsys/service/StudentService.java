package com.tutorsys.service;

import com.tutorsys.dto.StudentDto;
import com.tutorsys.dto.StudentRateDto;
import com.tutorsys.dto.SubjectDto;
import com.tutorsys.entity.Parent;
import com.tutorsys.entity.Student;
import com.tutorsys.entity.StudentRate;
import com.tutorsys.entity.Subject;
import com.tutorsys.repository.ParentRepository;
import com.tutorsys.repository.StudentRateRepository;
import com.tutorsys.repository.StudentRepository;
import com.tutorsys.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRateRepository studentRateRepository;

    public StudentService(StudentRepository studentRepository, ParentRepository parentRepository,
                          SubjectRepository subjectRepository, StudentRateRepository studentRateRepository) {
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.subjectRepository = subjectRepository;
        this.studentRateRepository = studentRateRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getAllStudents() {
        return studentRepository.findByDeletedFalse().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByParentId(Long parentId) {
        return studentRepository.findByParentIdAndDeletedFalse(parentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudentsByParentUsername(String username) {
        return studentRepository.findByParentUserUsernameAndDeletedFalse(username).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentDto getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + id));
        if (student.isDeleted()) {
            throw new IllegalArgumentException("Student has been deleted");
        }
        return convertToDto(student);
    }

    @Transactional
    public StudentDto createStudent(StudentDto dto) {
        Parent parent = parentRepository.findById(dto.getParentId())
                .orElseThrow(() -> new IllegalArgumentException("Parent not found with id: " + dto.getParentId()));

        Student student = new Student();
        student.setParent(parent);
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setPreferredName(dto.getPreferredName());
        student.setGrade(dto.getGrade());
        student.setSchool(dto.getSchool());
        student.setDateJoined(dto.getDateJoined() != null ? dto.getDateJoined() : LocalDate.now());
        student.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        student.setNotes(dto.getNotes());

        if (dto.getSubjects() != null) {
            Set<Subject> subjects = dto.getSubjects().stream()
                    .map(subDto -> subjectRepository.findById(subDto.getId())
                            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subDto.getId())))
                    .collect(Collectors.toSet());
            student.setSubjects(subjects);
        }

        student = studentRepository.save(student);
        return convertToDto(student);
    }

    @Transactional
    public StudentDto updateStudent(Long id, StudentDto dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + id));

        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setPreferredName(dto.getPreferredName());
        student.setGrade(dto.getGrade());
        student.setSchool(dto.getSchool());
        student.setStatus(dto.getStatus());
        student.setNotes(dto.getNotes());

        if (dto.getSubjects() != null) {
            Set<Subject> subjects = dto.getSubjects().stream()
                    .map(subDto -> subjectRepository.findById(subDto.getId())
                            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subDto.getId())))
                    .collect(Collectors.toSet());
            student.setSubjects(subjects);
        }

        student = studentRepository.save(student);
        return convertToDto(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + id));
        student.setDeleted(true);
        studentRepository.save(student);
    }

    // Pricing Rates Logic
    @Transactional(readOnly = true)
    public List<StudentRateDto> getStudentRates(Long studentId) {
        return studentRateRepository.findByStudentId(studentId).stream()
                .map(this::convertRateToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentRateDto addOrUpdateRate(StudentRateDto dto) {
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        // Close any existing active rate for this student and subject by setting its effectiveEndDate to the day before
        LocalDate newStart = dto.getEffectiveStartDate() != null ? dto.getEffectiveStartDate() : LocalDate.now();
        List<StudentRate> rates = studentRateRepository.findByStudentId(dto.getStudentId());
        for (StudentRate rate : rates) {
            if (rate.getSubject().getId().equals(dto.getSubjectId()) && rate.getEffectiveEndDate() == null) {
                rate.setEffectiveEndDate(newStart.minusDays(1));
                studentRateRepository.save(rate);
            }
        }

        StudentRate rate = new StudentRate();
        rate.setStudent(student);
        rate.setSubject(subject);
        rate.setRatePerSession(dto.getRatePerSession());
        rate.setEffectiveStartDate(newStart);
        rate.setEffectiveEndDate(dto.getEffectiveEndDate());

        rate = studentRateRepository.save(rate);
        return convertRateToDto(rate);
    }

    @Transactional(readOnly = true)
    public BigDecimal getStudentRateForSession(Long studentId, Long subjectId, LocalDate date) {
        Optional<StudentRate> rate = studentRateRepository.findRateForStudentAndSubjectAtDate(studentId, subjectId, date);
        if (rate.isPresent()) {
            return rate.get().getRatePerSession();
        }
        // Fallback default rate if not specified (e.g. $45)
        return new BigDecimal("45.00");
    }

    public StudentDto convertToDto(Student student) {
        StudentDto dto = new StudentDto();
        dto.setId(student.getId());
        dto.setParentId(student.getParent().getId());
        dto.setParentName(student.getParent().getName());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setPreferredName(student.getPreferredName());
        dto.setGrade(student.getGrade());
        dto.setSchool(student.getSchool());
        dto.setDateJoined(student.getDateJoined());
        dto.setStatus(student.getStatus());
        dto.setNotes(student.getNotes());
        dto.setSubjects(student.getSubjects().stream().map(sub -> {
            SubjectDto s = new SubjectDto();
            s.setId(sub.getId());
            s.setName(sub.getName());
            s.setDescription(sub.getDescription());
            s.setActive(sub.isActive());
            return s;
        }).collect(Collectors.toSet()));
        return dto;
    }

    private StudentRateDto convertRateToDto(StudentRate rate) {
        StudentRateDto dto = new StudentRateDto();
        dto.setId(rate.getId());
        dto.setStudentId(rate.getStudent().getId());
        dto.setStudentName(rate.getStudent().getFirstName() + " " + rate.getStudent().getLastName());
        dto.setSubjectId(rate.getSubject().getId());
        dto.setSubjectName(rate.getSubject().getName());
        dto.setRatePerSession(rate.getRatePerSession());
        dto.setEffectiveStartDate(rate.getEffectiveStartDate());
        dto.setEffectiveEndDate(rate.getEffectiveEndDate());
        return dto;
    }
}
