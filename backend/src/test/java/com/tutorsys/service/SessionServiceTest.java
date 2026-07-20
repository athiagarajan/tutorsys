package com.tutorsys.service;

import com.tutorsys.dto.SessionDto;
import com.tutorsys.entity.Invoice;
import com.tutorsys.entity.Session;
import com.tutorsys.entity.Student;
import com.tutorsys.entity.Subject;
import com.tutorsys.repository.InvoiceRepository;
import com.tutorsys.repository.SessionRepository;
import com.tutorsys.repository.StudentRepository;
import com.tutorsys.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private SessionService sessionService;

    private Student student;
    private Subject subject;
    private Session existingSession;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setFirstName("Alice");
        student.setLastName("Smith");

        subject = new Subject();
        subject.setId(2L);
        subject.setName("Math");

        existingSession = new Session();
        existingSession.setId(10L);
        existingSession.setStudent(student);
        existingSession.setSubject(subject);
        existingSession.setSessionDate(LocalDate.of(2026, 7, 20));
        existingSession.setActualStartTime(LocalTime.of(10, 0));
        existingSession.setActualDurationMinutes(60);
        existingSession.setStatus("CONDUCTED");
        existingSession.setDeleted(false);
    }

    @Test
    void testCreateSession_Success_NoOverlap() {
        SessionDto dto = new SessionDto();
        dto.setStudentId(1L);
        dto.setSubjectId(2L);
        dto.setSessionDate(LocalDate.of(2026, 7, 20));
        dto.setActualStartTime(LocalTime.of(11, 0)); // Starts exactly when the other ends
        dto.setActualDurationMinutes(60);
        dto.setStatus("CONDUCTED");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(subjectRepository.findById(2L)).thenReturn(Optional.of(subject));
        when(sessionRepository.findConductedAndMakeupSessionsByStudentAndDate(1L, LocalDate.of(2026, 7, 20)))
                .thenReturn(Collections.singletonList(existingSession));
        when(studentService.getStudentRateForSession(any(), any(), any())).thenReturn(new BigDecimal("50.00"));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session s = invocation.getArgument(0);
            s.setId(11L);
            return s;
        });

        SessionDto result = sessionService.createSession(dto);
        assertNotNull(result);
        assertEquals(11L, result.getId());
        assertNull(result.getNotificationMessage());
        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    @Test
    void testCreateSession_OverlapReplacesConductedSession() {
        SessionDto dto = new SessionDto();
        dto.setStudentId(1L);
        dto.setSubjectId(2L);
        dto.setSessionDate(LocalDate.of(2026, 7, 20));
        dto.setActualStartTime(LocalTime.of(10, 30)); // Overlaps with 10:00 - 11:00
        dto.setActualDurationMinutes(30);
        dto.setStatus("CONDUCTED");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(subjectRepository.findById(2L)).thenReturn(Optional.of(subject));
        when(sessionRepository.findConductedAndMakeupSessionsByStudentAndDate(1L, LocalDate.of(2026, 7, 20)))
                .thenReturn(Collections.singletonList(existingSession));
        when(studentService.getStudentRateForSession(any(), any(), any())).thenReturn(new BigDecimal("50.00"));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(11L);
            }
            return s;
        });

        SessionDto result = sessionService.createSession(dto);
        assertNotNull(result);
        assertEquals(11L, result.getId());
        assertNotNull(result.getNotificationMessage());
        assertTrue(result.getNotificationMessage().contains("replaced"));
        assertTrue(existingSession.isDeleted());
        verify(sessionRepository, atLeast(2)).save(any(Session.class)); // Once to delete the old, once to save the new
    }

    @Test
    void testUpdateSession_Success_SameSessionSelfOverlapIgnored() {
        SessionDto dto = new SessionDto();
        dto.setStudentId(1L);
        dto.setSubjectId(2L);
        dto.setSessionDate(LocalDate.of(2026, 7, 20));
        dto.setActualStartTime(LocalTime.of(10, 15)); // Adjust duration slightly within itself
        dto.setActualDurationMinutes(45);
        dto.setStatus("CONDUCTED");

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(existingSession));
        when(sessionRepository.findConductedAndMakeupSessionsByStudentAndDate(1L, LocalDate.of(2026, 7, 20)))
                .thenReturn(Collections.singletonList(existingSession));
        when(studentService.getStudentRateForSession(any(), any(), any())).thenReturn(new BigDecimal("50.00"));
        when(sessionRepository.save(any(Session.class))).thenReturn(existingSession);

        SessionDto result = sessionService.updateSession(10L, dto);
        assertNotNull(result);
        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    @Test
    void testDeleteSession_UnbilledSuccess() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(existingSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(existingSession);

        sessionService.deleteSession(10L);

        assertTrue(existingSession.isDeleted());
        verify(sessionRepository, times(1)).save(existingSession);
        verify(invoiceRepository, never()).delete(any(Invoice.class));
    }

    @Test
    void testDeleteSession_BilledSuccessDeletesInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId(100L);
        existingSession.setInvoice(invoice);

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(existingSession));
        when(sessionRepository.findByInvoiceIdAndDeletedFalse(100L)).thenReturn(Collections.singletonList(existingSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(existingSession);

        sessionService.deleteSession(10L);

        assertTrue(existingSession.isDeleted());
        assertNull(existingSession.getInvoice());
        verify(sessionRepository, times(2)).save(existingSession); // Once for disassociation, once for deletion status
        verify(invoiceRepository, times(1)).delete(invoice);
    }
}
