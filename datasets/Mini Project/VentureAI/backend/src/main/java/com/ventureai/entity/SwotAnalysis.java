package com.ventureai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "swot_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwotAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validation_id", nullable = false, unique = true)
    @JsonIgnore
    private Validation validation;

    @Column(columnDefinition = "TEXT")
    private String strengths; // JSON array of strings

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(columnDefinition = "TEXT")
    private String opportunities;

    @Column(columnDefinition = "TEXT")
    private String threats;

    @Column(nullable = false)
    @Builder.Default
    private Boolean aiGenerated = true;
}
