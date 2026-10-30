package com.ventureai.repository;

import com.ventureai.entity.AiInsight;
import com.ventureai.entity.Validation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiInsightRepository extends JpaRepository<AiInsight, Long> {
    Optional<AiInsight> findByValidation(Validation validation);
}
