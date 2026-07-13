package com.tutorsys.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ScheduleDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long subjectId;
    private String subjectName;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private int durationMinutes;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private boolean active;
}
