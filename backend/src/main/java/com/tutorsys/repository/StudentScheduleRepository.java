package com.tutorsys.repository;

import com.tutorsys.entity.StudentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudentScheduleRepository extends JpaRepository<StudentSchedule, Long> {
    List<StudentSchedule> findByStudentIdAndDeletedFalse(Long studentId);
    List<StudentSchedule> findByDeletedFalse();

    @Query("SELECT s FROM StudentSchedule s WHERE s.deleted = false AND s.active = true AND s.student.parent.user.username = :username")
    List<StudentSchedule> findByParentUsernameAndDeletedFalse(@Param("username") String username);

    @Query("SELECT s FROM StudentSchedule s WHERE s.student.id = :studentId AND s.deleted = false AND s.active = true " +
           "AND s.effectiveStartDate <= :date AND (s.effectiveEndDate IS NULL OR s.effectiveEndDate >= :date)")
    List<StudentSchedule> findActiveSchedulesForStudentAtDate(@Param("studentId") Long studentId, @Param("date") LocalDate date);
}
