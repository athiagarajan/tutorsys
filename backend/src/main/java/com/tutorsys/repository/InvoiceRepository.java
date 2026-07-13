package com.tutorsys.repository;

import com.tutorsys.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByParentId(Long parentId);
    List<Invoice> findByParentUserUsername(String username);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM Invoice i WHERE i.parent.id = :parentId AND i.status <> 'CANCELLED'")
    BigDecimal getOutstandingBalanceForParent(@Param("parentId") Long parentId);

    @Query("SELECT i FROM Invoice i WHERE i.parent.id = :parentId AND i.balanceDue > 0 AND i.status <> 'CANCELLED' ORDER BY i.dueDate ASC")
    List<Invoice> findUnpaidInvoicesForParentOrderedByDueDate(@Param("parentId") Long parentId);

    @Query("SELECT COALESCE(SUM(i.balanceDue), 0) FROM Invoice i WHERE i.status <> 'CANCELLED'")
    BigDecimal getTotalOutstandingDues();
}
