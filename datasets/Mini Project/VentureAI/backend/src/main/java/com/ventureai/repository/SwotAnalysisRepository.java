package com.ventureai.repository;

import com.ventureai.entity.SwotAnalysis;
import com.ventureai.entity.Validation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SwotAnalysisRepository extends JpaRepository<SwotAnalysis, Long> {
    Optional<SwotAnalysis> findByValidation(Validation validation);
}
