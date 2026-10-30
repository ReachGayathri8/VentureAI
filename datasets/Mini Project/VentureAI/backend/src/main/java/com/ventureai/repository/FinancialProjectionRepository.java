package com.ventureai.repository;

import com.ventureai.entity.FinancialProjection;
import com.ventureai.entity.Startup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinancialProjectionRepository extends JpaRepository<FinancialProjection, Long> {
    List<FinancialProjection> findByStartupOrderByPeriodAsc(Startup startup);
    void deleteByStartupId(Long startupId);
}
