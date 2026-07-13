package com.tutorsys.repository;

import com.tutorsys.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByParentId(Long parentId);
    List<Payment> findByParentUserUsername(String username);
    List<Payment> findByPaymentDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentDate BETWEEN :start AND :end")
    BigDecimal getTotalRevenueInPeriod(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
