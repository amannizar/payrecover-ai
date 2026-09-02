package com.payrecover.repository;

import com.payrecover.entity.RecoveryAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RecoveryActionRepository extends JpaRepository<RecoveryAction, Long> {

    List<RecoveryAction> findByPaymentId(String paymentId);

    List<RecoveryAction> findBySelectedAction(String action);

    List<RecoveryAction> findByActualResult(String result);

    @Query("SELECT ra.selectedAction, COUNT(ra) FROM RecoveryAction ra GROUP BY ra.selectedAction ORDER BY COUNT(ra) DESC")
    List<Object[]> countBySelectedAction();

    @Query("SELECT SUM(ra.actualRecovery) FROM RecoveryAction ra WHERE ra.actualResult = 'SUCCESS'")
    BigDecimal sumRecoveredAmount();

    @Query("SELECT COUNT(DISTINCT ra.paymentId) FROM RecoveryAction ra WHERE ra.actualResult = 'SUCCESS'")
    long countSuccessfulPayments();

    @Query("SELECT ra.stoppingReason, COUNT(ra) FROM RecoveryAction ra WHERE ra.stoppingReason != '' GROUP BY ra.stoppingReason")
    List<Object[]> countByStoppingReason();

    List<RecoveryAction> findByPaymentIdOrderByTimestampStepAsc(String paymentId);
}
