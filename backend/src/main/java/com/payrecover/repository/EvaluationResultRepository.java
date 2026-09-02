package com.payrecover.repository;

import com.payrecover.entity.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {

    Optional<EvaluationResult> findByStrategy(String strategy);

    @Query("SELECT e FROM EvaluationResult e ORDER BY e.createdAt DESC")
    List<EvaluationResult> findAllOrderByCreatedAtDesc();
}
