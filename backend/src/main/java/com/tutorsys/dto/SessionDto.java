package com.tutorsys.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class SessionDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long subjectId;
    private String subjectName;
    private LocalDate sessionDate;
    private LocalTime scheduledStartTime;
    private LocalTime actualStartTime;
    private Integer actualDurationMinutes;
    private String status;
    private BigDecimal rateCharged;
    private Long invoiceId;
    private String invoiceNumber;
    private String notes;
}
