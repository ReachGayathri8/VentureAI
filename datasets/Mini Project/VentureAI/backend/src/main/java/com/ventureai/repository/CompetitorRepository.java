package com.ventureai.repository;

import com.ventureai.entity.Competitor;
import com.ventureai.entity.Startup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetitorRepository extends JpaRepository<Competitor, Long> {
    List<Competitor> findByStartup(Startup startup);
    List<Competitor> findByStartupId(Long startupId);
    void deleteByStartupId(Long startupId);
}
