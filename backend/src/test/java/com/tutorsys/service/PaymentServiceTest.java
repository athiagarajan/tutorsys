package com.tutorsys.service;

import com.tutorsys.dto.PaymentDto;
import com.tutorsys.entity.Invoice;
import com.tutorsys.entity.Parent;
import com.tutorsys.entity.Payment;
import com.tutorsys.entity.PaymentAllocation;
import com.tutorsys.repository.InvoiceRepository;
import com.tutorsys.repository.ParentRepository;
import com.tutorsys.repository.PaymentAllocationRepository;
import com.tutorsys.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentAllocationRepository allocationRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Parent parent;

    @BeforeEach
    void setUp() {
        parent = new Parent();
        parent.setId(1L);
        parent.setName("John Doe");
    }

    @Test
    void testRecordPayment_ExactAmountForSingleInvoice() {
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setParentId(1L);
        paymentDto.setAmount(new BigDecimal("100.00"));
        paymentDto.setPaymentMethod("CREDIT_CARD");
        paymentDto.setPaymentDate(LocalDate.now());

        Invoice invoice = new Invoice();
        invoice.setId(10L);
        invoice.setParent(parent);
        invoice.setSubtotalAmount(new BigDecimal("100.00"));
        invoice.setPaymentsApplied(BigDecimal.ZERO);
        invoice.setBalanceDue(new BigDecimal("100.00"));
        invoice.setStatus("UNPAID");
        invoice.setDueDate(LocalDate.now().plusDays(5));

        when(parentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });
        when(invoiceRepository.findUnpaidInvoicesForParentOrderedByDueDate(1L)).thenReturn(List.of(invoice));

        PaymentDto result = paymentService.recordPayment(paymentDto);

        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.getAmount());

        // Check invoice was updated to PAID
        assertEquals(new BigDecimal("100.00"), invoice.getPaymentsApplied());
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getBalanceDue()));
        assertEquals("PAID", invoice.getStatus());

        // Verify allocation was created
        verify(allocationRepository, times(1)).save(any(PaymentAllocation.class));
        verify(invoiceRepository, times(1)).save(invoice);
    }

    @Test
    void testRecordPayment_FIFOAllocationAcrossMultipleInvoices() {
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setParentId(1L);
        paymentDto.setAmount(new BigDecimal("150.00")); // Enough to pay invoice 1 fully and invoice 2 partially
        paymentDto.setPaymentMethod("CASH");

        Invoice invoice1 = new Invoice();
        invoice1.setId(10L);
        invoice1.setParent(parent);
        invoice1.setSubtotalAmount(new BigDecimal("100.00"));
        invoice1.setPaymentsApplied(BigDecimal.ZERO);
        invoice1.setBalanceDue(new BigDecimal("100.00"));
        invoice1.setStatus("UNPAID");
        invoice1.setDueDate(LocalDate.now().plusDays(5));

        Invoice invoice2 = new Invoice();
        invoice2.setId(11L);
        invoice2.setParent(parent);
        invoice2.setSubtotalAmount(new BigDecimal("100.00"));
        invoice2.setPaymentsApplied(BigDecimal.ZERO);
        invoice2.setBalanceDue(new BigDecimal("100.00"));
        invoice2.setStatus("UNPAID");
        invoice2.setDueDate(LocalDate.now().plusDays(15));

        List<Invoice> unpaidInvoices = new ArrayList<>();
        unpaidInvoices.add(invoice1);
        unpaidInvoices.add(invoice2);

        when(parentRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });
        when(invoiceRepository.findUnpaidInvoicesForParentOrderedByDueDate(1L)).thenReturn(unpaidInvoices);

        paymentService.recordPayment(paymentDto);

        // Invoice 1 should be fully paid
        assertEquals(new BigDecimal("100.00"), invoice1.getPaymentsApplied());
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice1.getBalanceDue()));
        assertEquals("PAID", invoice1.getStatus());

        // Invoice 2 should be partially paid (50.00 applied)
        assertEquals(new BigDecimal("50.00"), invoice2.getPaymentsApplied());
        assertEquals(new BigDecimal("50.00"), invoice2.getBalanceDue());
        assertEquals("UNPAID", invoice2.getStatus());

        // Verify two allocations were saved
        verify(allocationRepository, times(2)).save(any(PaymentAllocation.class));
        verify(invoiceRepository, times(2)).save(any(Invoice.class));
    }
}
