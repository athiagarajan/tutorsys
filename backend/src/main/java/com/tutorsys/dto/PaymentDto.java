package com.tutorsys.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PaymentDto {
    private Long id;
    private Long parentId;
    private String parentName;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String paymentMethod;
    private String referenceNumber;
    private String notes;
}
