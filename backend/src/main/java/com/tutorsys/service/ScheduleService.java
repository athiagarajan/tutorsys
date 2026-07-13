package com.tutorsys.service;

import com.tutorsys.dto.ScheduleDto;
import com.tutorsys.entity.Student;
import com.tutorsys.entity.StudentSchedule;
import com.tutorsys.entity.Subject;
import com.tutorsys.repository.StudentRepository;
import com.tutorsys.repository.StudentScheduleRepository;
import com.tutorsys.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private final StudentScheduleRepository scheduleRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;

    public ScheduleService(StudentScheduleRepository scheduleRepository, StudentRepository studentRepository,
                           SubjectRepository subjectRepository) {
        this.scheduleRepository = scheduleRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleDto> getAllSchedules() {
        return scheduleRepository.findByDeletedFalse().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleDto> getSchedulesByStudentId(Long studentId) {
        return scheduleRepository.findByStudentIdAndDeletedFalse(studentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleDto> getSchedulesByParentUsername(String username) {
        return scheduleRepository.findByParentUsernameAndDeletedFalse(username).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScheduleDto createSchedule(ScheduleDto dto) {
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        StudentSchedule schedule = new StudentSchedule();
        schedule.setStudent(student);
        schedule.setSubject(subject);
        schedule.setDayOfWeek(dto.getDayOfWeek().toUpperCase());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setDurationMinutes(dto.getDurationMinutes());
        schedule.setEffectiveStartDate(dto.getEffectiveStartDate());
        schedule.setEffectiveEndDate(dto.getEffectiveEndDate());
        schedule.setActive(true);

        schedule = scheduleRepository.save(schedule);
        return convertToDto(schedule);
    }

    @Transactional
    public ScheduleDto updateSchedule(Long id, ScheduleDto dto) {
        StudentSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));

        schedule.setDayOfWeek(dto.getDayOfWeek().toUpperCase());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setDurationMinutes(dto.getDurationMinutes());
        schedule.setEffectiveStartDate(dto.getEffectiveStartDate());
        schedule.setEffectiveEndDate(dto.getEffectiveEndDate());
        schedule.setActive(dto.isActive());

        schedule = scheduleRepository.save(schedule);
        return convertToDto(schedule);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        StudentSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        schedule.setDeleted(true);
        scheduleRepository.save(schedule);
    }

    public ScheduleDto convertToDto(StudentSchedule schedule) {
        ScheduleDto dto = new ScheduleDto();
        dto.setId(schedule.getId());
        dto.setStudentId(schedule.getStudent().getId());
        dto.setStudentName(schedule.getStudent().getFirstName() + " " + schedule.getStudent().getLastName());
        dto.setSubjectId(schedule.getSubject().getId());
        dto.setSubjectName(schedule.getSubject().getName());
        dto.setDayOfWeek(schedule.getDayOfWeek());
        dto.setStartTime(schedule.getStartTime());
        dto.setEndTime(schedule.getEndTime());
        dto.setDurationMinutes(schedule.getDurationMinutes());
        dto.setEffectiveStartDate(schedule.getEffectiveStartDate());
        dto.setEffectiveEndDate(schedule.getEffectiveEndDate());
        dto.setActive(schedule.isActive());
        return dto;
    }
}
