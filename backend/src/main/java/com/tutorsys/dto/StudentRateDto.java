package com.tutorsys.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class StudentRateDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long subjectId;
    private String subjectName;
    private BigDecimal ratePerSession;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
}
