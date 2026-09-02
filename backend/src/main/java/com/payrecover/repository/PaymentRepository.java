package com.payrecover.repository;

import com.payrecover.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    List<Payment> findByStatus(String status);

    List<Payment> findByFailureCode(String failureCode);

    List<Payment> findByFailureCategory(String failureCategory);

    List<Payment> findByPaymentMethod(String paymentMethod);

    @Query("SELECT p FROM Payment p WHERE p.paymentId LIKE %:search% OR p.customerId LIKE %:search%")
    List<Payment> search(String search);

    @Query("SELECT p.failureCategory, COUNT(p) FROM Payment p GROUP BY p.failureCategory")
    List<Object[]> countByFailureCategory();

    @Query("SELECT p.failureCode, COUNT(p) FROM Payment p GROUP BY p.failureCode ORDER BY COUNT(p) DESC")
    List<Object[]> countByFailureCode();

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p")
    Double sumAllAmounts();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    Double sumAmountByStatus(String status);
}
