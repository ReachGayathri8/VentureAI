package com.ventureai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Holds every Gemini/AI-generated section beyond SWOT: competitor
 * intelligence narrative, market opportunity analysis, personalized
 * recommendations and risk analysis. Each column stores a JSON payload as
 * text (MySQL TEXT), parsed back into structured DTOs by the service layer.
 * This is an extension beyond the minimal schema sketch in the spec,
 * consistent with its "create tables such as..." wording.
 */
@Entity
@Table(name = "ai_insights")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_id", nullable = false, unique = true)
    @JsonIgnore
    private Validation validation;

    @Column(columnDefinition = "TEXT")
    private String competitiveAdvantage;

    @Column(columnDefinition = "TEXT")
    private String marketGap;

    @Column(columnDefinition = "TEXT")
    private String differentiationStrategy;

    @Column(columnDefinition = "TEXT")
    private String marketOpportunitiesJson; // JSON array

    @Column(columnDefinition = "TEXT")
    private String recommendationsJson; // JSON object keyed by category

    @Column(columnDefinition = "TEXT")
    private String risksJson; // JSON array of risk objects

    @Column(columnDefinition = "TEXT")
    private String finalVerdict;

    @Builder.Default
    private Boolean aiGenerated = true;
}
