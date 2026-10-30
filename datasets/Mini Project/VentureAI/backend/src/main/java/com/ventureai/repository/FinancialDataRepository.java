package com.ventureai.repository;

import com.ventureai.entity.FinancialData;
import com.ventureai.entity.Startup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinancialDataRepository extends JpaRepository<FinancialData, Long> {
    Optional<FinancialData> findByStartup(Startup startup);
    Optional<FinancialData> findByStartupId(Long startupId);
}
