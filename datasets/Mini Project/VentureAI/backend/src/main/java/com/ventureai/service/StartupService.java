package com.ventureai.service;

import com.ventureai.dto.startup.CompetitorDto;
import com.ventureai.dto.startup.CompetitorRequest;
import com.ventureai.dto.startup.StartupRequest;
import com.ventureai.dto.startup.StartupResponse;
import com.ventureai.entity.*;
import com.ventureai.entity.enums.CompetitorSource;
import com.ventureai.exception.ResourceNotFoundException;
import com.ventureai.repository.*;
import com.ventureai.service.ai.GeminiAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StartupService {

    private final StartupRepository startupRepository;
    private final CompetitorRepository competitorRepository;
    private final FinancialDataRepository financialDataRepository;
    private final FinancialProjectionRepository financialProjectionRepository;
    private final ValidationRepository validationRepository;
    private final ReportRepository reportRepository;
    private final GeminiAiService geminiAiService;


    // ============================================================
    // CREATE STARTUP
    // ============================================================

    @Transactional
    public Startup createStartup(User user, StartupRequest req) {

        Startup startup =
                Startup.builder()
                        .user(user)
                        .name(req.startupName())
                        .industry(req.industry())
                        .businessCategory(req.businessCategory())
                        .startupIdea(req.startupIdea())
                        .description(req.businessDescription())
                        .problem(req.problem())
                        .solution(req.solution())
                        .targetMarket(req.targetMarket())
                        .targetCustomers(req.targetCustomers())
                        .businessModel(req.businessModel())
                        .revenueModel(req.revenueModel())
                        .uniqueValueProposition(req.uniqueValueProposition())
                        .marketingStrategy(req.marketingStrategy())
                        .location(req.location())
                        .founderExperience(req.founderExperience())
                        .autoDiscoverCompetitors(
                                Boolean.TRUE.equals(req.autoDiscoverCompetitors())
                        )
                        .build();

        startup = startupRepository.save(startup);


        // ========================================================
        // FINANCIAL DATA
        // ========================================================

        FinancialData fd =
                FinancialData.builder()
                        .startup(startup)
                        .initialInvestment(nz(req.initialInvestment()))
                        .founderInvestment(nz(req.founderInvestment()))
                        .externalFunding(nz(req.externalFunding()))
                        .monthlyOperatingCost(nz(req.monthlyOperatingCost()))
                        .employeeCost(nz(req.employeeCost()))
                        .marketingCost(nz(req.marketingCost()))
                        .infrastructureCost(nz(req.infrastructureCost()))
                        .productDevelopmentCost(nz(req.productDevelopmentCost()))
                        .expectedMonthlyCustomers(
                                req.expectedMonthlyCustomers() == null
                                        ? 0
                                        : req.expectedMonthlyCustomers()
                        )
                        .averageCustomerRevenue(nz(req.averageCustomerRevenue()))
                        .expectedGrowthRate(
                                req.expectedGrowthRate() == null
                                        ? 0.0
                                        : req.expectedGrowthRate()
                        )
                        .projectionPeriodYears(
                                req.projectionPeriodYears() == null
                                        ? 3
                                        : req.projectionPeriodYears()
                        )
                        .fixedCosts(nz(req.fixedCosts()))
                        .variableCostPerUnit(nz(req.variableCostPerUnit()))
                        .sellingPricePerUnit(nz(req.sellingPricePerUnit()))
                        .build();

        financialDataRepository.save(fd);


        // ========================================================
        // MANUAL COMPETITORS
        // ========================================================

        List<Competitor> competitors = new ArrayList<>();

        addManualCompetitors(startup, req.competitors(), competitors);


        // ========================================================
        // SAVE MANUAL COMPETITORS ONLY
        // ========================================================
        //
        // Automatic discovery is intentionally NOT executed here.
        // It must happen only inside ValidationService, after the
        // startup input quality gate has passed. This prevents junk
        // submissions from triggering Tavily/web research.
        //

        if (!competitors.isEmpty()) {
            competitorRepository.saveAll(competitors);
        }

        return startup;
    }


    // ============================================================
    // LIST STARTUPS
    // ============================================================

    @Transactional(readOnly = true)
    public List<StartupResponse> listStartups(User user) {
        return startupRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // GET OWNED STARTUP
    // ============================================================

    @Transactional(readOnly = true)
    public Startup getOwnedStartup(User user, Long startupId) {
        return startupRepository
                .findByIdAndUser(startupId, user)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Startup not found.")
                );
    }


    // ============================================================
    // GET STARTUP RESPONSE
    // ============================================================

    @Transactional(readOnly = true)
    public StartupResponse getStartupResponse(User user, Long startupId) {
        return toResponse(getOwnedStartup(user, startupId));
    }


    // ============================================================
    // DELETE STARTUP
    // ============================================================

    @Transactional
    public void deleteStartup(User user, Long startupId) {

        Startup startup =
                startupRepository
                        .findByIdAndUser(startupId, user)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Startup not found.")
                        );

        Long id = startup.getId();

        // --------------------------------------------------------
        // 1. Find all validations
        // --------------------------------------------------------

        List<Validation> validations =
                validationRepository
                        .findByStartupOrderByCreatedAtDesc(startup);

        // --------------------------------------------------------
        // 2. Delete reports first
        // --------------------------------------------------------

        for (Validation validation : validations) {
            reportRepository.deleteByValidationId(validation.getId());
        }

        reportRepository.flush();

        // --------------------------------------------------------
        // 3. Delete financial projections
        // --------------------------------------------------------

        financialProjectionRepository.deleteByStartupId(id);
        financialProjectionRepository.flush();

        // --------------------------------------------------------
        // 4. Delete validations
        // --------------------------------------------------------

        for (Validation validation : validations) {
            validationRepository.delete(validation);
        }

        validationRepository.flush();

        // --------------------------------------------------------
        // 5. Delete competitors
        // --------------------------------------------------------

        competitorRepository.deleteByStartupId(id);
        competitorRepository.flush();

        // --------------------------------------------------------
        // 6. Delete financial data
        // --------------------------------------------------------

        financialDataRepository
                .findByStartupId(id)
                .ifPresent(financialData -> {
                    financialDataRepository.delete(financialData);
                    financialDataRepository.flush();
                });

        // --------------------------------------------------------
        // 7. Delete startup
        // --------------------------------------------------------

        startupRepository.delete(startup);
        startupRepository.flush();
    }


    // ============================================================
    // UPDATE STARTUP
    // ============================================================

    @Transactional
    public StartupResponse updateStartup(
            User user,
            Long startupId,
            StartupRequest req
    ) {

        Startup startup = getOwnedStartup(user, startupId);

        // --------------------------------------------------------
        // 1. Update startup/business information
        // --------------------------------------------------------

        startup.setName(req.startupName());
        startup.setIndustry(req.industry());
        startup.setBusinessCategory(req.businessCategory());
        startup.setStartupIdea(req.startupIdea());
        startup.setDescription(req.businessDescription());
        startup.setProblem(req.problem());
        startup.setSolution(req.solution());
        startup.setTargetMarket(req.targetMarket());
        startup.setTargetCustomers(req.targetCustomers());
        startup.setBusinessModel(req.businessModel());
        startup.setRevenueModel(req.revenueModel());
        startup.setUniqueValueProposition(req.uniqueValueProposition());
        startup.setMarketingStrategy(req.marketingStrategy());
        startup.setLocation(req.location());
        startup.setFounderExperience(req.founderExperience());
        startup.setAutoDiscoverCompetitors(
                Boolean.TRUE.equals(req.autoDiscoverCompetitors())
        );

        startupRepository.save(startup);


        // --------------------------------------------------------
        // 2. Update financial data
        // --------------------------------------------------------

        FinancialData fd =
                financialDataRepository
                        .findByStartupId(startupId)
                        .orElseGet(() ->
                                FinancialData.builder()
                                        .startup(startup)
                                        .build()
                        );

        fd.setStartup(startup);
        fd.setInitialInvestment(nz(req.initialInvestment()));
        fd.setFounderInvestment(nz(req.founderInvestment()));
        fd.setExternalFunding(nz(req.externalFunding()));
        fd.setMonthlyOperatingCost(nz(req.monthlyOperatingCost()));
        fd.setEmployeeCost(nz(req.employeeCost()));
        fd.setMarketingCost(nz(req.marketingCost()));
        fd.setInfrastructureCost(nz(req.infrastructureCost()));
        fd.setProductDevelopmentCost(nz(req.productDevelopmentCost()));
        fd.setExpectedMonthlyCustomers(
                req.expectedMonthlyCustomers() == null
                        ? 0
                        : req.expectedMonthlyCustomers()
        );
        fd.setAverageCustomerRevenue(nz(req.averageCustomerRevenue()));
        fd.setExpectedGrowthRate(
                req.expectedGrowthRate() == null
                        ? 0.0
                        : req.expectedGrowthRate()
        );
        fd.setProjectionPeriodYears(
                req.projectionPeriodYears() == null
                        ? 3
                        : req.projectionPeriodYears()
        );
        fd.setFixedCosts(nz(req.fixedCosts()));
        fd.setVariableCostPerUnit(nz(req.variableCostPerUnit()));
        fd.setSellingPricePerUnit(nz(req.sellingPricePerUnit()));

        financialDataRepository.save(fd);


        // --------------------------------------------------------
        // 3. Replace manual competitors
        // --------------------------------------------------------
        //
        // Existing automatically discovered competitors are removed
        // before refreshing them. Manual competitors are also rebuilt
        // from the current Edit form, so deleted/renamed manual
        // competitors do not remain in the database.
        // --------------------------------------------------------

        competitorRepository.deleteByStartupId(startupId);
        competitorRepository.flush();

        List<Competitor> competitors = new ArrayList<>();

        addManualCompetitors(startup, req.competitors(), competitors);

        // --------------------------------------------------------
        // 4. Save current manual competitors
        // --------------------------------------------------------
        //
        // Automatic discovery is intentionally deferred to
        // ValidationService so the input quality gate runs first.
        //

        if (!competitors.isEmpty()) {
            competitorRepository.saveAll(competitors);
            competitorRepository.flush();
        }

        return toResponse(startup);
    }


    // ============================================================
    // MANUAL COMPETITOR HELPER
    // ============================================================

    private void addManualCompetitors(
            Startup startup,
            List<CompetitorRequest> requests,
            List<Competitor> target
    ) {

        if (requests == null || requests.isEmpty()) {
            return;
        }

        for (CompetitorRequest c : requests) {

            if (c == null || c.name() == null || c.name().isBlank()) {
                continue;
            }

            target.add(
                    Competitor.builder()
                            .startup(startup)
                            .name(c.name().trim())
                            .website(c.website())
                            .description(c.description())
                            .source(CompetitorSource.MANUAL)
                            .priceRating(3)
                            .featureRating(3)
                            .marketReachRating(3)
                            .innovationRating(3)
                            .build()
            );
        }
    }


    // ============================================================
    // RESPONSE
    // ============================================================

    private StartupResponse toResponse(Startup s) {

        List<CompetitorDto> competitors =
                competitorRepository
                        .findByStartupId(s.getId())
                        .stream()
                        .map(c ->
                                new CompetitorDto(
                                        c.getId(),
                                        c.getName(),
                                        c.getWebsite(),
                                        c.getDescription(),
                                        c.getPricing(),
                                        c.getStrengths(),
                                        c.getWeaknesses(),
                                        c.getMarketPosition(),
                                        c.getSource() == null
                                                ? CompetitorSource.MANUAL.name()
                                                : c.getSource().name(),
                                        c.getPriceRating(),
                                        c.getFeatureRating(),
                                        c.getMarketReachRating(),
                                        c.getInnovationRating()
                                )
                        )
                        .toList();

        Optional<FinancialData> financialData =
                financialDataRepository.findByStartupId(s.getId());

        Optional<Validation> latest =
                validationRepository
                        .findTopByStartupOrderByCreatedAtDesc(s);

        FinancialData fd = financialData.orElse(null);

        return new StartupResponse(
                // Basic information
                s.getId(),
                s.getName(),
                s.getIndustry(),
                s.getBusinessCategory(),
                s.getStartupIdea(),
                s.getDescription(),
                s.getProblem(),
                s.getSolution(),
                s.getTargetMarket(),
                s.getTargetCustomers(),
                s.getBusinessModel(),
                s.getRevenueModel(),
                s.getUniqueValueProposition(),
                s.getMarketingStrategy(),
                s.getLocation(),

                // Founder / competitor settings
                s.getFounderExperience(),
                Boolean.TRUE.equals(s.getAutoDiscoverCompetitors()),

                // Financial information
                fd == null ? BigDecimal.ZERO : nz(fd.getInitialInvestment()),
                fd == null ? BigDecimal.ZERO : nz(fd.getFounderInvestment()),
                fd == null ? BigDecimal.ZERO : nz(fd.getExternalFunding()),
                fd == null ? BigDecimal.ZERO : nz(fd.getMonthlyOperatingCost()),
                fd == null ? BigDecimal.ZERO : nz(fd.getEmployeeCost()),
                fd == null ? BigDecimal.ZERO : nz(fd.getMarketingCost()),
                fd == null ? BigDecimal.ZERO : nz(fd.getInfrastructureCost()),
                fd == null ? BigDecimal.ZERO : nz(fd.getProductDevelopmentCost()),
                fd == null ? 0 : safeInt(fd.getExpectedMonthlyCustomers()),
                fd == null ? BigDecimal.ZERO : nz(fd.getAverageCustomerRevenue()),
                fd == null ? 0.0 : safeDouble(fd.getExpectedGrowthRate()),
                fd == null ? 3 : safeInt(fd.getProjectionPeriodYears()),
                fd == null ? BigDecimal.ZERO : nz(fd.getFixedCosts()),
                fd == null ? BigDecimal.ZERO : nz(fd.getVariableCostPerUnit()),
                fd == null ? BigDecimal.ZERO : nz(fd.getSellingPricePerUnit()),

                // Metadata
                s.getCreatedAt(),

                // Competitors
                competitors,

                // Latest validation
                latest
                        .map(Validation::getViabilityScore)
                        .orElse(null),

                latest
                        .map(v ->
                                v.getClassification() == null
                                        ? null
                                        : v.getClassification().name()
                        )
                        .orElse(null),

                latest
                        .map(v ->
                                v.getRiskLevel() == null
                                        ? null
                                        : v.getRiskLevel().name()
                        )
                        .orElse(null)
        );
    }


    // ============================================================
    // NULL-SAFE HELPERS
    // ============================================================

    private BigDecimal nz(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }
}

