package com.ventureai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "startups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Startup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String industry;

    @Column(length = 150)
    private String businessCategory;

    @Column(columnDefinition = "TEXT")
    private String startupIdea;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String problem;

    @Column(columnDefinition = "TEXT")
    private String solution;

    @Column(length = 200)
    private String targetMarket;

    /**
     * Specific customer groups the startup intends to serve.
     *
     * This is kept separate from targetMarket so the Edit Startup
     * form can restore the exact Target Customers field.
     */
    @Column(columnDefinition = "TEXT")
    private String targetCustomers;

    @Column(columnDefinition = "TEXT")
    private String businessModel;

    @Column(columnDefinition = "TEXT")
    private String revenueModel;

    @Column(columnDefinition = "TEXT")
    private String uniqueValueProposition;

    @Column(columnDefinition = "TEXT")
    private String marketingStrategy;

    @Column(length = 150)
    private String location;

    /**
     * Optional 0-100 founder/business experience signal used by the XGBoost model.
     */
    private Double founderExperience;

    /**
     * Controls whether VentureAI automatically searches for
     * real competitors using the external web-search service.
     *
     * false = manual competitors only
     * true  = automatic real-world competitor discovery enabled
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean autoDiscoverCompetitors = false;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonIgnore
    @OneToMany(mappedBy = "startup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Competitor> competitors = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "startup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Validation> validations = new ArrayList<>();

    @JsonIgnore
    @OneToOne(mappedBy = "startup", cascade = CascadeType.ALL, orphanRemoval = true)
    private FinancialData financialData;
}
