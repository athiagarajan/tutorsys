package com.tutorsys.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class InvoiceDto {
    private Long id;
    private Long parentId;
    private String parentName;
    private String invoiceNumber;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String status;
    private BigDecimal subtotalAmount;
    private BigDecimal previousBalance;
    private BigDecimal paymentsApplied;
    private BigDecimal balanceDue;
    private String pdfFilePath;
    private String notes;
    private List<SessionDto> sessions;
}
