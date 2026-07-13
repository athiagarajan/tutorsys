package com.tutorsys.service;

import com.tutorsys.dto.SessionDto;
import com.tutorsys.entity.Session;
import com.tutorsys.entity.Student;
import com.tutorsys.entity.Subject;
import com.tutorsys.repository.SessionRepository;
import com.tutorsys.repository.StudentRepository;
import com.tutorsys.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final StudentService studentService;

    public SessionService(SessionRepository sessionRepository, StudentRepository studentRepository,
                          SubjectRepository subjectRepository, StudentService studentService) {
        this.sessionRepository = sessionRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.studentService = studentService;
    }

    @Transactional(readOnly = true)
    public List<SessionDto> getAllSessions() {
        return sessionRepository.findByDeletedFalse().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SessionDto> getSessionsByStudentId(Long studentId) {
        return sessionRepository.findByStudentIdAndDeletedFalse(studentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SessionDto> getSessionsByParentUsername(String username) {
        return sessionRepository.findByParentUsernameAndDeletedFalse(username).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SessionDto> getSessionsInPeriod(LocalDate start, LocalDate end) {
        return sessionRepository.findBySessionDateBetweenAndDeletedFalse(start, end).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SessionDto createSession(SessionDto dto) {
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        Session session = new Session();
        session.setStudent(student);
        session.setSubject(subject);
        session.setSessionDate(dto.getSessionDate());
        session.setScheduledStartTime(dto.getScheduledStartTime());
        session.setActualStartTime(dto.getActualStartTime() != null ? dto.getActualStartTime() : dto.getScheduledStartTime());
        session.setActualDurationMinutes(dto.getActualDurationMinutes() != null ? dto.getActualDurationMinutes() : 60);
        session.setStatus(dto.getStatus().toUpperCase());
        session.setNotes(dto.getNotes());

        // Look up pricing rate for this student, subject, and session date
        BigDecimal rate = studentService.getStudentRateForSession(dto.getStudentId(), dto.getSubjectId(), dto.getSessionDate());
        session.setRateCharged(rate);

        session = sessionRepository.save(session);
        return convertToDto(session);
    }

    @Transactional
    public SessionDto updateSession(Long id, SessionDto dto) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        session.setSessionDate(dto.getSessionDate());
        session.setScheduledStartTime(dto.getScheduledStartTime());
        session.setActualStartTime(dto.getActualStartTime());
        session.setActualDurationMinutes(dto.getActualDurationMinutes());
        session.setStatus(dto.getStatus().toUpperCase());
        session.setNotes(dto.getNotes());

        if (dto.getRateCharged() != null) {
            session.setRateCharged(dto.getRateCharged());
        } else {
            BigDecimal rate = studentService.getStudentRateForSession(session.getStudent().getId(), session.getSubject().getId(), dto.getSessionDate());
            session.setRateCharged(rate);
        }

        session = sessionRepository.save(session);
        return convertToDto(session);
    }

    @Transactional
    public void deleteSession(Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        session.setDeleted(true);
        sessionRepository.save(session);
    }

    public SessionDto convertToDto(Session session) {
        SessionDto dto = new SessionDto();
        dto.setId(session.getId());
        dto.setStudentId(session.getStudent().getId());
        dto.setStudentName(session.getStudent().getFirstName() + " " + session.getStudent().getLastName());
        dto.setSubjectId(session.getSubject().getId());
        dto.setSubjectName(session.getSubject().getName());
        dto.setSessionDate(session.getSessionDate());
        dto.setScheduledStartTime(session.getScheduledStartTime());
        dto.setActualStartTime(session.getActualStartTime());
        dto.setActualDurationMinutes(session.getActualDurationMinutes());
        dto.setStatus(session.getStatus());
        dto.setRateCharged(session.getRateCharged());
        dto.setNotes(session.getNotes());
        if (session.getInvoice() != null) {
            dto.setInvoiceId(session.getInvoice().getId());
            dto.setInvoiceNumber(session.getInvoice().getInvoiceNumber());
        }
        return dto;
    }
}
