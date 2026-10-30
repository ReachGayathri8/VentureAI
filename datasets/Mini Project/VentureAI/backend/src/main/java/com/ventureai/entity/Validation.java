package com.ventureai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ventureai.entity.enums.Classification;
import com.ventureai.entity.enums.RiskLevel;
import com.ventureai.entity.enums.ValidationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "validations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Validation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // ============================================================
    // STARTUP
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id", nullable = false)
    @JsonIgnore
    private Startup startup;


    // ============================================================
    // VALIDATION STATUS
    // ============================================================
    //
    // VALID
    // INVALID_INPUT
    // INSUFFICIENT_DATA
    // FAILED
    //
    // Only VALID validations should receive a viability score
    // and be eligible for comparison.
    //

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ValidationStatus status = ValidationStatus.VALID;


    // ============================================================
    // VIABILITY / AI SCORES
    // ============================================================

    private Double viabilityScore;

    private Double confidence;

    private Double riskScore;

    private Double marketScore;

    private Double businessModelScore;

    private Double financialScore;

    private Double scalabilityScore;

    private Double competitionScore;


    // ============================================================
    // CLASSIFICATION
    // ============================================================

    @Enumerated(EnumType.STRING)
    private Classification classification;


    // ============================================================
    // RISK
    // ============================================================

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;


    // ============================================================
    // NLP INFORMATION
    // ============================================================

    @Column(columnDefinition = "TEXT")
    private String detectedIndustry;

    @Column(columnDefinition = "TEXT")
    private String extractedKeywords;


    // ============================================================
    // CREATED DATE
    // ============================================================

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt =
            LocalDateTime.now();


    // ============================================================
    // SWOT
    // ============================================================

    @JsonIgnore
    @OneToOne(
            mappedBy = "validation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private SwotAnalysis swotAnalysis;


    // ============================================================
    // AI INSIGHTS
    // ============================================================

    @JsonIgnore
    @OneToOne(
            mappedBy = "validation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private AiInsight aiInsight;
}