package com.tutorsys.repository;

import com.tutorsys.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByDeletedFalse();
    List<Session> findByStudentIdAndDeletedFalse(Long studentId);
    List<Session> findBySessionDateBetweenAndDeletedFalse(LocalDate start, LocalDate end);

    @Query("SELECT s FROM Session s WHERE s.deleted = false AND s.student.parent.user.username = :username")
    List<Session> findByParentUsernameAndDeletedFalse(@Param("username") String username);

    @Query("SELECT s FROM Session s WHERE s.student.id = :studentId AND s.deleted = false AND s.invoice IS NULL " +
           "AND s.sessionDate <= :endDate AND s.status IN ('CONDUCTED', 'ABSENT_STUDENT', 'MAKEUP')")
    List<Session> findBillableSessionsForStudentBeforeDate(
            @Param("studentId") Long studentId,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT s FROM Session s WHERE s.student.parent.id = :parentId AND s.deleted = false AND s.invoice IS NULL " +
           "AND s.sessionDate BETWEEN :start AND :end AND s.status IN ('CONDUCTED', 'ABSENT_STUDENT', 'MAKEUP')")
    List<Session> findBillableSessionsForParentInPeriod(
            @Param("parentId") Long parentId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    List<Session> findByInvoiceIdAndDeletedFalse(Long invoiceId);

    @Query("SELECT s FROM Session s WHERE s.student.id = :studentId AND s.sessionDate = :sessionDate AND s.status = :status AND s.deleted = false")
    List<Session> findByStudentIdAndSessionDateAndStatusAndDeletedFalse(
            @Param("studentId") Long studentId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("status") String status
    );
}
