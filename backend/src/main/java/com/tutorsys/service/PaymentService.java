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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ParentRepository parentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentAllocationRepository allocationRepository;

    public PaymentService(PaymentRepository paymentRepository, ParentRepository parentRepository,
                          InvoiceRepository invoiceRepository, PaymentAllocationRepository allocationRepository) {
        this.paymentRepository = paymentRepository;
        this.parentRepository = parentRepository;
        this.invoiceRepository = invoiceRepository;
        this.allocationRepository = allocationRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByParentUsername(String username) {
        return paymentRepository.findByParentUserUsername(username).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByParentId(Long parentId) {
        return paymentRepository.findByParentId(parentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentDto recordPayment(PaymentDto dto) {
        Parent parent = parentRepository.findById(dto.getParentId())
                .orElseThrow(() -> new IllegalArgumentException("Parent not found"));

        Payment payment = new Payment();
        payment.setParent(parent);
        payment.setPaymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDate.now());
        payment.setAmount(dto.getAmount());
        payment.setPaymentMethod(dto.getPaymentMethod().toUpperCase());
        payment.setReferenceNumber(dto.getReferenceNumber());
        payment.setNotes(dto.getNotes());

        payment = paymentRepository.save(payment);

        // Balance Allocation Algorithm (FIFO based on invoice due date)
        BigDecimal remainingAmount = payment.getAmount();
        List<Invoice> unpaidInvoices = invoiceRepository.findUnpaidInvoicesForParentOrderedByDueDate(parent.getId());

        for (Invoice invoice : unpaidInvoices) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal balanceDue = invoice.getBalanceDue();
            BigDecimal allocation = remainingAmount.min(balanceDue);

            // Create allocation record
            PaymentAllocation allocationRecord = new PaymentAllocation();
            allocationRecord.setPayment(payment);
            allocationRecord.setInvoice(invoice);
            allocationRecord.setAmountAllocated(allocation);
            allocationRepository.save(allocationRecord);

            // Update invoice balances
            invoice.setPaymentsApplied(invoice.getPaymentsApplied().add(allocation));
            invoice.setBalanceDue(invoice.getBalanceDue().subtract(allocation));

            if (invoice.getBalanceDue().compareTo(BigDecimal.ZERO) == 0) {
                invoice.setStatus("PAID");
            }
            invoiceRepository.save(invoice);

            remainingAmount = remainingAmount.subtract(allocation);
        }

        return convertToDto(payment);
    }

    public PaymentDto convertToDto(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());
        dto.setParentId(payment.getParent().getId());
        dto.setParentName(payment.getParent().getName());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setReferenceNumber(payment.getReferenceNumber());
        dto.setNotes(payment.getNotes());
        return dto;
    }
}
