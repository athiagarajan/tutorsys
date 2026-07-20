package com.tutorsys.service;

import com.tutorsys.dto.SessionDto;
import com.tutorsys.entity.Session;
import com.tutorsys.entity.Student;
import com.tutorsys.entity.Subject;
import com.tutorsys.entity.Invoice;
import com.tutorsys.repository.InvoiceRepository;
import com.tutorsys.repository.SessionRepository;
import com.tutorsys.repository.StudentRepository;
import com.tutorsys.repository.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final StudentService studentService;
    private final InvoiceRepository invoiceRepository;

    public SessionService(SessionRepository sessionRepository, StudentRepository studentRepository,
                          SubjectRepository subjectRepository, StudentService studentService,
                          InvoiceRepository invoiceRepository) {
        this.sessionRepository = sessionRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.studentService = studentService;
        this.invoiceRepository = invoiceRepository;
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

    private String handleOverlapReplacement(Long studentId, LocalDate sessionDate, LocalTime startTime, Integer durationMinutes, Long excludeSessionId) {
        if (startTime == null || durationMinutes == null || studentId == null || sessionDate == null) {
            return null;
        }

        List<Session> existingConducted = sessionRepository.findConductedAndMakeupSessionsByStudentAndDate(studentId, sessionDate);
        LocalTime newStart = startTime;
        LocalTime newEnd = startTime.plusMinutes(durationMinutes);
        StringBuilder notification = new StringBuilder();

        for (Session existing : existingConducted) {
            if (excludeSessionId != null && existing.getId().equals(excludeSessionId)) {
                continue;
            }

            LocalTime extStart = existing.getActualStartTime();
            LocalTime extEnd = extStart.plusMinutes(existing.getActualDurationMinutes());

            // Check for overlap: newStart < extEnd && extStart < newEnd
            if (newStart.isBefore(extEnd) && extStart.isBefore(newEnd)) {
                String invoiceInfo = "";
                if (existing.getInvoice() != null) {
                    Invoice invoice = existing.getInvoice();
                    invoiceInfo = " (associated with invoice " + invoice.getInvoiceNumber() + ")";
                    
                    // Disassociate all other sessions linked to this invoice
                    List<Session> linkedSessions = sessionRepository.findByInvoiceIdAndDeletedFalse(invoice.getId());
                    for (Session s : linkedSessions) {
                        s.setInvoice(null);
                        sessionRepository.save(s);
                    }
                    
                    // Delete invoice PDF file if exists
                    if (invoice.getPdfFilePath() != null) {
                        try {
                            new java.io.File(invoice.getPdfFilePath()).delete();
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                    
                    // Delete invoice itself
                    invoiceRepository.delete(invoice);
                }

                existing.setDeleted(true);
                sessionRepository.save(existing);

                if (notification.length() > 0) {
                    notification.append("; ");
                }
                notification.append(String.format("Overlapping session from %s to %s%s was replaced", extStart, extEnd, invoiceInfo));
            }
        }

        return notification.length() > 0 ? notification.toString() : null;
    }

    @Transactional
    public SessionDto createSession(SessionDto dto) {
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        LocalTime actualStart = dto.getActualStartTime() != null ? dto.getActualStartTime() : dto.getScheduledStartTime();
        Integer duration = dto.getActualDurationMinutes() != null ? dto.getActualDurationMinutes() : 60;
        String status = dto.getStatus().toUpperCase();

        String notificationMsg = null;
        if ("CONDUCTED".equals(status) || "MAKEUP".equals(status)) {
            notificationMsg = handleOverlapReplacement(dto.getStudentId(), dto.getSessionDate(), actualStart, duration, null);
        }

        Session session = new Session();
        session.setStudent(student);
        session.setSubject(subject);
        session.setSessionDate(dto.getSessionDate());
        session.setScheduledStartTime(dto.getScheduledStartTime());
        session.setActualStartTime(actualStart);
        session.setActualDurationMinutes(duration);
        session.setStatus(status);
        session.setNotes(dto.getNotes());

        // Look up pricing rate for this student, subject, and session date
        BigDecimal rate = studentService.getStudentRateForSession(dto.getStudentId(), dto.getSubjectId(), dto.getSessionDate());
        session.setRateCharged(rate);

        session = sessionRepository.save(session);
        SessionDto responseDto = convertToDto(session);
        responseDto.setNotificationMessage(notificationMsg);
        return responseDto;
    }

    @Transactional
    public SessionDto updateSession(Long id, SessionDto dto) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        LocalTime actualStart = dto.getActualStartTime() != null ? dto.getActualStartTime() : session.getActualStartTime();
        Integer duration = dto.getActualDurationMinutes() != null ? dto.getActualDurationMinutes() : session.getActualDurationMinutes();
        String status = dto.getStatus() != null ? dto.getStatus().toUpperCase() : session.getStatus();
        LocalDate date = dto.getSessionDate() != null ? dto.getSessionDate() : session.getSessionDate();

        String notificationMsg = null;
        if ("CONDUCTED".equals(status) || "MAKEUP".equals(status)) {
            notificationMsg = handleOverlapReplacement(session.getStudent().getId(), date, actualStart, duration, id);
        }

        session.setSessionDate(date);
        session.setScheduledStartTime(dto.getScheduledStartTime() != null ? dto.getScheduledStartTime() : session.getScheduledStartTime());
        session.setActualStartTime(actualStart);
        session.setActualDurationMinutes(duration);
        session.setStatus(status);
        session.setNotes(dto.getNotes());

        if (dto.getRateCharged() != null) {
            session.setRateCharged(dto.getRateCharged());
        } else {
            BigDecimal rate = studentService.getStudentRateForSession(session.getStudent().getId(), session.getSubject().getId(), date);
            session.setRateCharged(rate);
        }

        session = sessionRepository.save(session);
        SessionDto responseDto = convertToDto(session);
        responseDto.setNotificationMessage(notificationMsg);
        return responseDto;
    }

    @Transactional
    public void deleteSession(Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (session.getInvoice() != null) {
            Invoice invoice = session.getInvoice();
            
            // Disassociate all other sessions linked to this invoice
            List<Session> linkedSessions = sessionRepository.findByInvoiceIdAndDeletedFalse(invoice.getId());
            for (Session s : linkedSessions) {
                s.setInvoice(null);
                sessionRepository.save(s);
            }
            
            // Delete invoice PDF file if exists
            if (invoice.getPdfFilePath() != null) {
                try {
                    new java.io.File(invoice.getPdfFilePath()).delete();
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            // Delete invoice itself
            invoiceRepository.delete(invoice);
        }

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
