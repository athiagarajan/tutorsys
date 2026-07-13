package com.tutorsys.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "sessions")
@Getter
@Setter
public class Session extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "scheduled_start_time")
    private LocalTime scheduledStartTime;

    @Column(name = "actual_start_time")
    private LocalTime actualStartTime;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @Column(nullable = false, length = 30)
    private String status; // 'CONDUCTED', 'CANCELLED', 'ABSENT_STUDENT', 'ABSENT_TEACHER', 'HOLIDAY', 'MAKEUP'

    @Column(name = "rate_charged", precision = 10, scale = 2)
    private BigDecimal rateCharged;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
