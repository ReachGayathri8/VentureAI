package com.ventureai.repository;

import com.ventureai.entity.Report;
import com.ventureai.entity.Validation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByValidation(Validation validation);

    Optional<Report> findByValidationId(Long validationId);

    void deleteByValidationId(Long validationId);
}
