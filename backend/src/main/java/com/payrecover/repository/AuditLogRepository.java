package com.payrecover.repository;

import com.payrecover.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByPaymentIdOrderByCreatedAtDesc(String paymentId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByResult(String result);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countByAction();

    @Query("SELECT a.result, COUNT(a) FROM AuditLog a GROUP BY a.result ORDER BY COUNT(a) DESC")
    List<Object[]> countByResult();

    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC")
    List<AuditLog> findAllOrderByCreatedAtDesc();

    @Query(value = "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 20", nativeQuery = true)
    List<AuditLog> findTop20OrderByCreatedAtDesc();
}
