package com.ventureai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ventureai.entity.enums.CompetitorSource;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "competitors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id", nullable = false)
    @JsonIgnore
    private Startup startup;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 300)
    private String website;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String pricing;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(length = 150)
    private String marketPosition;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CompetitorSource source = CompetitorSource.MANUAL;

    // 1-5 star ratings used for the competitor comparison chart
    @Builder.Default
    private Integer priceRating = 3;
    @Builder.Default
    private Integer featureRating = 3;
    @Builder.Default
    private Integer marketReachRating = 3;
    @Builder.Default
    private Integer innovationRating = 3;
}
