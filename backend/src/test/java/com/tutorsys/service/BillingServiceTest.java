package com.tutorsys.service;

import com.tutorsys.dto.InvoiceDto;
import com.tutorsys.entity.Invoice;
import com.tutorsys.entity.Parent;
import com.tutorsys.entity.Session;
import com.tutorsys.entity.Student;
import com.tutorsys.entity.Subject;
import com.tutorsys.repository.InvoiceRepository;
import com.tutorsys.repository.ParentRepository;
import com.tutorsys.repository.SessionRepository;
import com.tutorsys.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BillingServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private BillingService billingService;

    private Parent parent;
    private Student student;
    private Session session;

    @BeforeEach
    void setUp() {
        parent = new Parent();
        parent.setId(1L);
        parent.setName("Jane Doe");
        parent.setEmail("jane@example.com");

        student = new Student();
        student.setId(2L);
        student.setParent(parent);
        student.setFirstName("Alice");
        student.setLastName("Doe");

        Subject subject = new Subject();
        subject.setId(3L);
        subject.setName("Mathematics");

        session = new Session();
        session.setId(4L);
        session.setStudent(student);
        session.setSubject(subject);
        session.setSessionDate(LocalDate.now());
        session.setRateCharged(new BigDecimal("50.00"));

        ReflectionTestUtils.setField(billingService, "invoicesDir", "./target/invoices");
    }

    @Test
    void testGenerateInvoice_Success() {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        when(parentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(invoiceRepository.getOutstandingBalanceForParent(1L)).thenReturn(new BigDecimal("20.00"));
        when(studentRepository.findByParentIdAndDeletedFalse(1L)).thenReturn(List.of(student));
        when(sessionRepository.findBillableSessionsForParentInPeriod(1L, start, end)).thenReturn(List.of(session));
        
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId(100L);
            return inv;
        });

        InvoiceDto result = billingService.generateInvoice(1L, start, end);

        assertNotNull(result);
        assertEquals(new BigDecimal("50.00"), result.getSubtotalAmount());
        assertEquals(new BigDecimal("20.00"), result.getPreviousBalance());
        assertEquals(new BigDecimal("70.00"), result.getBalanceDue()); // 50 subtotal + 20 previous
        assertEquals("DRAFT", result.getStatus());
        assertEquals(1L, result.getParentId());

        verify(invoiceRepository, times(2)).save(any(Invoice.class));
    }

    @Test
    void testGenerateInvoice_NoSessions() {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        when(parentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(invoiceRepository.getOutstandingBalanceForParent(1L)).thenReturn(BigDecimal.ZERO);
        when(studentRepository.findByParentIdAndDeletedFalse(1L)).thenReturn(List.of(student));
        when(sessionRepository.findBillableSessionsForParentInPeriod(1L, start, end)).thenReturn(Collections.emptyList());

        assertThrows(IllegalArgumentException.class, () -> {
            billingService.generateInvoice(1L, start, end);
        }, "Should throw exception if no billable sessions exist");
    }
}
