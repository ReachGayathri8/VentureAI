package com.ventureai.repository;

import com.ventureai.entity.Startup;
import com.ventureai.entity.Validation;
import com.ventureai.entity.enums.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ValidationRepository
        extends JpaRepository<Validation, Long> {

    List<Validation>
    findByStartupOrderByCreatedAtDesc(
            Startup startup
    );

    Optional<Validation>
    findTopByStartupOrderByCreatedAtDesc(
            Startup startup
    );

    List<Validation>
    findByStartupUserIdOrderByCreatedAtDesc(
            Long userId
    );

    List<Validation>
    findByStartupUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            ValidationStatus status
    );

    boolean existsByStartupId(
            Long startupId
    );
}
