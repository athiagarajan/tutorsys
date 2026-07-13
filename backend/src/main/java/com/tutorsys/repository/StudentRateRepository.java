package com.tutorsys.repository;

import com.tutorsys.entity.StudentRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRateRepository extends JpaRepository<StudentRate, Long> {
    List<StudentRate> findByStudentId(Long studentId);

    @Query("SELECT r FROM StudentRate r WHERE r.student.id = :studentId AND r.subject.id = :subjectId " +
           "AND r.effectiveStartDate <= :date AND (r.effectiveEndDate IS NULL OR r.effectiveEndDate >= :date)")
    Optional<StudentRate> findRateForStudentAndSubjectAtDate(
            @Param("studentId") Long studentId,
            @Param("subjectId") Long subjectId,
            @Param("date") LocalDate date
    );
}
