package com.ventureai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureai.dto.ai.AiAnalysisBundle;
import com.ventureai.dto.validation.ScoreBreakdownDto;
import com.ventureai.dto.validation.ValidationHistoryItemDto;
import com.ventureai.dto.validation.ValidationResultResponse;
import com.ventureai.entity.*;
import com.ventureai.entity.enums.Classification;
import com.ventureai.entity.enums.RiskLevel;
import com.ventureai.entity.enums.CompetitorSource;
import com.ventureai.exception.BadRequestException;
import com.ventureai.exception.ResourceNotFoundException;
import com.ventureai.repository.*;
import com.ventureai.service.ai.GeminiAiService;
import com.ventureai.service.finance.FinanceEngineService;
import com.ventureai.service.ml.MlClientService;
import com.ventureai.service.ml.generative.GenerativeAiClient;
import com.ventureai.service.ml.NlpExtractionResult;
import com.ventureai.service.ml.MlPredictionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * VentureAI Validation Pipeline
 *
 * Flow:
 *
 * USER INPUT
 *      ↓
 * INPUT QUALITY GATE
 *      ↓
 * NLP PROCESSING
 *      ↓
 * BART
 *      ↓
 * AI BUSINESS INTELLIGENCE
 *      ↓
 * FINANCIAL ENGINE
 *      ↓
 * VALIDATION RESULT
 *
 * Invalid or meaningless business information is rejected
 * before AI analysis starts.
 */
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final StartupRepository startupRepository;
    private final CompetitorRepository competitorRepository;
    private final FinancialDataRepository financialDataRepository;
    private final FinancialProjectionRepository financialProjectionRepository;
    private final ValidationRepository validationRepository;
    private final SwotAnalysisRepository swotAnalysisRepository;
    private final AiInsightRepository aiInsightRepository;

    private final MlClientService mlClientService;
    private final GeminiAiService geminiAiService;
    private final GenerativeAiClient generativeAiClient;
    private final FinanceEngineService financeEngineService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /*
     * Basic text patterns used by the input quality gate.
     */
    private static final Pattern LETTER_PATTERN =
            Pattern.compile("[A-Za-z]");

    private static final Pattern WORD_PATTERN =
            Pattern.compile("[A-Za-z]{2,}");


    // ============================================================
    // MAIN VALIDATION PIPELINE
    // ============================================================

    @Transactional
    public ValidationResultResponse runValidation(
            User user,
            Long startupId
    ) {

        Startup startup =
                startupRepository
                        .findByIdAndUser(
                                startupId,
                                user
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Startup not found."
                                )
                        );

        FinancialData fd =
                financialDataRepository
                        .findByStartup(startup)
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Financial inputs are incomplete. " +
                                        "Please complete Step 3 of the validation form."
                                )
                        );

        List<Competitor> storedCompetitors =
                competitorRepository.findByStartup(startup);

        /*
         * Competitor mode is persisted through CompetitorSource:
         *
         * MANUAL         -> keep exactly as entered by the user.
         * AUTO_DISCOVERED -> automatic discovery was enabled for this startup.
         *
         * We remove previous AUTO_DISCOVERED records before re-discovery so
         * stale/fake records from an older run can never be reused.
         */
        /*
         * Competitor mode is stored directly on Startup.
         *
         * This is the source of truth because automatic discovery may
         * legitimately return zero verified competitors. In that case
         * there will be no AUTO_DISCOVERED row to infer the user's choice
         * from.
         */
        boolean automaticCompetitorDiscovery =
                Boolean.TRUE.equals(
                        startup.getAutoDiscoverCompetitors()
                );

        List<Competitor> manualCompetitors =
                storedCompetitors.stream()
                        .filter(c ->
                                c.getSource() != CompetitorSource.AUTO_DISCOVERED
                        )
                        .toList();

        List<Competitor> competitors =
                new java.util.ArrayList<>(manualCompetitors);


        // ========================================================
        // 0. INPUT QUALITY GATE
        // ========================================================
        //
        // IMPORTANT:
        // This happens BEFORE NLP, BART and Gemini.
        //
        // If the input is obviously junk, the pipeline stops here.
        //
        validateStartupInput(
                startup,
                fd
        );


        // ========================================================
        // 1. NLP PREPROCESSING
        // ========================================================

        NlpExtractionResult nlp =
                mlClientService.extractNlpFeatures(
                        startup
                );

        if (
                startup.getIndustry() == null ||
                startup.getIndustry().isBlank()
        ) {
            startup.setIndustry(
                    nlp.industry()
            );
        }


        // ========================================================
        // 1.5 COMPETITOR DISCOVERY
        // ========================================================
        //
        // Automatic mode is inferred from the persisted AUTO_DISCOVERED
        // records created when the user selected "Automatically discover
        // competitors". Manual competitors are always preserved exactly.
        //
        // Discovery happens only after the input quality gate, so junk
        // startup data can never trigger competitor research.
        //
        if (automaticCompetitorDiscovery) {

            storedCompetitors.stream()
                    .filter(c ->
                            c.getSource() == CompetitorSource.AUTO_DISCOVERED
                    )
                    .forEach(
                            competitorRepository::delete
                    );

            /*
             * Flush the deletes before inserting the new discovery results.
             * This prevents stale AUTO_DISCOVERED rows from being reused by
             * downstream queries during the same validation transaction.
             */
            competitorRepository.flush();

            List<Competitor> discoveredCompetitors =
                    geminiAiService.discoverCompetitors(
                            startup,
                            3
                    );

            if (discoveredCompetitors != null &&
                    !discoveredCompetitors.isEmpty()) {

                competitors.addAll(
                        discoveredCompetitors.stream()
                                .filter(java.util.Objects::nonNull)
                                .filter(c ->
                                        c.getName() != null &&
                                        !c.getName().isBlank()
                                )
                                .toList()
                );
            }
        }

        /*
         * Save only the current competitor set. Manual records remain
         * untouched; automatic records are replaced by the latest discovery.
         */
        if (!competitors.isEmpty()) {
            competitorRepository.saveAll(competitors);
            competitorRepository.flush();
        }


        // ========================================================
        // 2. FEATURE ENGINEERING
        // ========================================================

        double marketSize =
        estimateMarketSize(
                fd,
                nlp
        );

double competitionLevel =
        estimateCompetitionLevel(
                competitors
        );

double growthRate =
        fd.getExpectedGrowthRate() == null
                ? 15
                : fd.getExpectedGrowthRate();

double customerDemand =
        (
                nlp.feature(
                        "problem_severity",
                        50
                )
                +
                nlp.feature(
                        "unique_value_prop_score",
                        50
                )
        ) / 2.0;

double founderExperience =
        startup.getFounderExperience() == null
                ? 50
                : startup.getFounderExperience();

/*
 * Investment adequacy for BART.
 *
 * This estimates whether the available startup funding
 * provides a reasonable operating runway.
 *
 * It is intentionally capped at 100.
 */
double initialInvestment =
        fd.getInitialInvestment() == null
                ? 0
                : fd.getInitialInvestment().doubleValue();

double monthlyOperatingCost =
        nonZero(
                fd.getMonthlyOperatingCost(),
                fd
        ).doubleValue();

double runwayMonths =
        monthlyOperatingCost > 0
                ? initialInvestment / monthlyOperatingCost
                : 0;

/*
 * Convert runway into a 0-100 adequacy score.
 *
 * Approximately:
 *  3 months  -> ~28
 *  6 months  -> ~49
 *  9 months  -> ~63
 * 12 months  -> ~74
 * 18+ months -> ~86+
 */
double investmentAdequacy =
        monthlyOperatingCost <= 0
                ? 50
                : Math.max(
                        0,
                        Math.min(
                                100,
                                100 * (
                                        1 - Math.exp(
                                                -runwayMonths / 9.0
                                        )
                                )
                        )
                );


        Map<String, Object> features =
                new HashMap<>();

        features.put(
                "industry",
                startup.getIndustry()
        );

        features.put(
                "initial_investment",
                fd.getInitialInvestment()
        );

        features.put(
                "monthly_operating_cost",
                nonZero(
                        fd.getMonthlyOperatingCost(),
                        fd
                )
        );

        features.put(
            "investment_adequacy",
                investmentAdequacy
        );

        features.put(
                "market_size",
                marketSize
        );

        features.put(
                "competition_level",
                competitionLevel
        );

        features.put(
                "expected_growth_rate",
                growthRate
        );

        features.put(
                "customer_demand",
                customerDemand
        );

        features.put(
                "business_model_strength",
                nlp.feature(
                        "business_model_clarity",
                        50
                )
        );

        features.put(
                "scalability",
                nlp.feature(
                        "scalability_signal",
                        50
                )
        );

        features.put(
                "founder_experience",
                founderExperience
        );

        features.put(
                "problem_severity",
                nlp.feature(
                        "problem_severity",
                        50
                )
        );

        features.put(
                "unique_value_prop_score",
                nlp.feature(
                        "unique_value_prop_score",
                        50
                )
        );


        // ========================================================
        // 3. BART PREDICTION
        // ========================================================

        MlPredictionResult prediction =
                mlClientService.predictViability(
                        features
                );


        // ========================================================
        // 4. AI BUSINESS INTELLIGENCE
        // ========================================================

        System.out.println("=== BART COMPLETE - STARTING GEMINI ANALYSIS ===");

        AiAnalysisBundle ai =
                geminiAiService.generateFullAnalysis(
                        startup,
                        competitors,
                        nlp,
                        prediction,
                        marketSize,
                        competitionLevel,
                        growthRate
                );


        // ========================================================
        // 5. FINANCIAL ENGINE
        // ========================================================

        System.out.println("=== GEMINI COMPLETE - STARTING FINANCIAL ENGINE ===");

        int projectionYears =
                fd.getProjectionPeriodYears() == null
                        ? 3
                        : fd.getProjectionPeriodYears();

        if (projectionYears < 1 || projectionYears > 10) {
            projectionYears = 3;
        }

        int totalMonths = projectionYears * 12;

        System.out.println(
                "FINANCE: projectionYears=" + projectionYears
                        + ", totalMonths=" + totalMonths
                        + ", startupId=" + startup.getId()
        );

        System.out.println("FINANCE: generating monthly projections...");

        List<FinancialProjection> projections =
                financeEngineService
                        .computeMonthlyProjections(
                                startup,
                                fd,
                                totalMonths
                        );

        if (projections == null) {
            projections = List.of();
        }

        System.out.println(
                "FINANCE: generated " + projections.size()
                        + " projection rows"
        );

        if (!projections.isEmpty()) {
            System.out.println("FINANCE: deleting old projection rows...");

            financialProjectionRepository
                    .deleteByStartupId(
                            startup.getId()
                    );

            System.out.println("FINANCE: saving projection rows...");

            financialProjectionRepository
                    .saveAll(
                            projections
                    );

            System.out.println("FINANCE: projection rows saved");
        } else {
            System.out.println(
                    "FINANCE: no projection rows generated; "
                            + "continuing with empty financial data"
            );
        }

        System.out.println("FINANCE: calculating financial summary...");

        var financialSummary =
                financeEngineService.summarize(
                        fd,
                        projections
                );

        double financialScore =
                scoreFinancialHealth(
                        financialSummary,
                        prediction
                );

        if (Double.isNaN(financialScore)
                || Double.isInfinite(financialScore)) {
            financialScore = 0.0;
        }

        financialScore =
                Math.max(
                        0.0,
                        Math.min(
                                100.0,
                                financialScore
                        )
                );

        System.out.println(
                "FINANCE: financialScore=" + financialScore
        );

        System.out.println("=== FINANCIAL ENGINE COMPLETE ===");


        // ========================================================
        // 6. BUILD VALIDATION
        // ========================================================

        double scalabilityScore =
                nlp.feature(
                        "scalability_signal",
                        50
                );

        double businessModelScore =
                nlp.feature(
                        "business_model_clarity",
                        50
                );

        RiskLevel riskLevel =
                RiskLevel.fromScore(
                        competitionLevel,
                        100 - marketSize
                );

        Classification classification =
                resolveClassification(
                        prediction.classification(),
                        prediction.score()
                );


        System.out.println(
                "VALIDATION: BART classification="
                        + prediction.classification()
                        + ", score="
                        + prediction.score()
                        + " -> database classification="
                        + classification
        );

        Validation validation =
                Validation.builder()
                        .startup(startup)
                        .viabilityScore(
                                prediction.score()
                        )
                        .confidence(
                                prediction.confidence()
                        )
                        .riskScore(
                                competitionLevel
                        )
                        .marketScore(
                                marketSize
                        )
                        .businessModelScore(
                                businessModelScore
                        )
                        .financialScore(
                                financialScore
                        )
                        .scalabilityScore(
                                scalabilityScore
                        )
                        .competitionScore(
                                competitionLevel
                        )
                        .classification(
                                classification
                        )
                        .riskLevel(
                                riskLevel
                        )
                        .detectedIndustry(
                                contextAwareIndustry(
                                        startup.getIndustry(),
                                        nlp.industry()
                                )
                        )
                        .extractedKeywords(
                                writeJson(
                                        nlp.keywords()
                                )
                        )
                        .build();

        try {
            System.out.println("VALIDATION: saving validation row...");

            validation =
                    validationRepository.save(
                            validation
                    );

            System.out.println(
                    "VALIDATION: validation saved, id=" + validation.getId()
            );


            // ========================================================
            // 7. SWOT
            // ========================================================

            System.out.println("VALIDATION: building SWOT...");

            SwotAnalysis swot =
                SwotAnalysis.builder()
                        .validation(validation)
                        .strengths(
                                writeJson(
                                        ai.swot().strengths()
                                )
                        )
                        .weaknesses(
                                writeJson(
                                        ai.swot().weaknesses()
                                )
                        )
                        .opportunities(
                                writeJson(
                                        ai.swot().opportunities()
                                )
                        )
                        .threats(
                                writeJson(
                                        ai.swot().threats()
                                )
                        )
                        .aiGenerated(
                                ai.liveAiGenerated()
                        )
                        .build();

            System.out.println("VALIDATION: saving SWOT...");

            swotAnalysisRepository.save(
                    swot
            );

            System.out.println("VALIDATION: SWOT saved");


            // ========================================================
            // 8. AI INSIGHTS
            // ========================================================

            System.out.println("VALIDATION: building AI insights...");

            AiInsight insight =
                AiInsight.builder()
                        .validation(validation)
                        .competitiveAdvantage(
                                ai.competitiveAdvantage()
                        )
                        .marketGap(
                                ai.marketGap()
                        )
                        .differentiationStrategy(
                                ai.differentiationStrategy()
                        )
                        .marketOpportunitiesJson(
                                writeJson(
                                        ai.marketOpportunity()
                                )
                        )
                        .recommendationsJson(
                                writeJson(
                                        ai.recommendations()
                                )
                        )
                        .risksJson(
                                writeJson(
                                        ai.riskAnalysis()
                                )
                        )
                        .finalVerdict(
                                ai.finalVerdict()
                        )
                        .aiGenerated(
                                ai.liveAiGenerated()
                        )
                        .build();

            System.out.println("VALIDATION: saving AI insights...");

            aiInsightRepository.save(
                    insight
            );

            System.out.println("VALIDATION: AI insights saved");


            // ========================================================
            // RETURN RESULT
            // ========================================================

            System.out.println("=== DATABASE SAVE COMPLETE - BUILDING RESPONSE ===");
            System.out.println("VALIDATION: building response DTO...");

            ValidationResultResponse response =
                    toResultResponse(
                            validation,
                            startup
                    );

            System.out.println("VALIDATION: response DTO built successfully");
            System.out.println("=== VALIDATION PIPELINE COMPLETE ===");

            return response;

        } catch (RuntimeException validationException) {
            System.err.println("=== VALIDATION PIPELINE FAILED AFTER FINANCIAL ENGINE ===");
            System.err.println(
                    "VALIDATION ERROR TYPE: "
                            + validationException.getClass().getName()
            );
            System.err.println(
                    "VALIDATION ERROR: "
                            + validationException.getMessage()
            );
            validationException.printStackTrace();
            throw validationException;
        }
    }


    // ============================================================
    // BART CLASSIFICATION RESOLUTION
    // ============================================================

    /**
     * Converts the human-readable classification returned by BART into the
     * exact Classification enum constant used by the backend.
     *
     * BART may return values such as:
     *   Viable
     *   VIABLE
     *   Not Viable
     *   NOT_VIABLE
     *   not-viable
     *
     * Enum.valueOf() is intentionally not used directly because it requires
     * an exact, case-sensitive match.
     */
    private Classification resolveClassification(
            String bartLabel,
            double score
    ) {
        /*
         * BART is a binary viability model:
         *     Viable / Not Viable
         *
         * The database Classification enum is a four-level business
         * classification:
         *     HIGH_POTENTIAL
         *     MODERATE_POTENTIAL
         *     NEEDS_IMPROVEMENT
         *     HIGH_RISK
         *
         * Therefore the BART label must NOT be passed directly to
         * Enum.valueOf(). We use BART's continuous 0-100 score to
         * map the result to the application's four business levels.
         *
         * Score bands:
         *     75-100 -> HIGH_POTENTIAL
         *     50-74  -> MODERATE_POTENTIAL
         *     30-49  -> NEEDS_IMPROVEMENT
         *     0-29   -> HIGH_RISK
         *
         * This preserves BART as the only viability predictor and
         * avoids inventing a classification enum value.
         */

        if (!Double.isFinite(score)) {
            throw new IllegalStateException(
                    "BART returned an invalid viability score: " + score
            );
        }

        double safeScore =
                Math.max(
                        0.0,
                        Math.min(
                                100.0,
                                score
                        )
                );

        if (safeScore >= 75.0) {
            return Classification.HIGH_POTENTIAL;
        }

        if (safeScore >= 50.0) {
            return Classification.MODERATE_POTENTIAL;
        }

        if (safeScore >= 30.0) {
            return Classification.NEEDS_IMPROVEMENT;
        }

        return Classification.HIGH_RISK;
    }


    // ============================================================
    // INPUT QUALITY GATE
    // ============================================================

    private void validateStartupInput(
            Startup startup,
            FinancialData fd
    ) {

        // --------------------------------------------------------
        // Required startup information
        // --------------------------------------------------------

        requireText(
                startup.getName(),
                "Startup name is required."
        );

        requireText(
                startup.getStartupIdea(),
                "Please provide a clear startup idea."
        );

        requireText(
                startup.getProblem(),
                "Please describe the problem your startup solves."
        );

        requireText(
                startup.getSolution(),
                "Please describe your proposed solution."
        );

        /*
         * The current Startup entity stores this information
         * as targetMarket.
         *
         * The frontend calls it Target Customers, but the current
         * entity does not have getTargetCustomers().
         */
        requireText(
                startup.getTargetMarket(),
                "Please specify your target customers or target market."
        );

        requireText(
                startup.getRevenueModel(),
                "Please specify how your startup will make money."
        );

        requireText(
                startup.getIndustry(),
                "Please select an industry."
        );

        requireText(
                startup.getBusinessModel(),
                "Please select a business model."
        );


        // --------------------------------------------------------
        // Meaningful text validation
        // --------------------------------------------------------

        validateMeaningfulText(
                startup.getStartupIdea(),
                "Startup idea"
        );

        validateMeaningfulText(
                startup.getProblem(),
                "Problem description"
        );

        validateMeaningfulText(
                startup.getSolution(),
                "Solution description"
        );

        validateMeaningfulText(
                startup.getTargetMarket(),
                "Target market / customers"
        );

        validateMeaningfulText(
                startup.getRevenueModel(),
                "Revenue model"
        );


        // --------------------------------------------------------
        // Business coherence
        // --------------------------------------------------------

        int strongDescriptions = 0;

        if (
                hasMeaningfulDescription(
                        startup.getStartupIdea()
                )
        ) {
            strongDescriptions++;
        }

        if (
                hasMeaningfulDescription(
                        startup.getProblem()
                )
        ) {
            strongDescriptions++;
        }

        if (
                hasMeaningfulDescription(
                        startup.getSolution()
                )
        ) {
            strongDescriptions++;
        }

        if (strongDescriptions < 2) {

            throw new BadRequestException(
                    "The business information is too vague. " +
                    "Please provide a clear idea, problem and solution " +
                    "that explain what the business actually does."
            );
        }


        // --------------------------------------------------------
        // Financial sanity checks
        // --------------------------------------------------------

        validateNonNegative(
                fd.getInitialInvestment(),
                "Initial investment"
        );

        validateNonNegative(
                fd.getFounderInvestment(),
                "Founder investment"
        );

        validateNonNegative(
                fd.getExternalFunding(),
                "External funding"
        );

        validateNonNegative(
                fd.getMonthlyOperatingCost(),
                "Monthly operating cost"
        );

        validateNonNegative(
                fd.getEmployeeCost(),
                "Employee cost"
        );

        validateNonNegative(
                fd.getMarketingCost(),
                "Marketing cost"
        );

        validateNonNegative(
                fd.getInfrastructureCost(),
                "Infrastructure cost"
        );

        validateNonNegative(
                fd.getProductDevelopmentCost(),
                "Product development cost"
        );

        validateNonNegative(
                fd.getExpectedMonthlyCustomers(),
                "Expected monthly customers"
        );

        validateNonNegative(
                fd.getAverageCustomerRevenue(),
                "Average customer revenue"
        );

        validateNonNegative(
                fd.getExpectedGrowthRate(),
                "Expected growth rate"
        );

        validateNonNegative(
                fd.getFixedCosts(),
                "Fixed costs"
        );

        validateNonNegative(
                fd.getVariableCostPerUnit(),
                "Variable cost per unit"
        );

        validateNonNegative(
                fd.getSellingPricePerUnit(),
                "Selling price per unit"
        );


        // --------------------------------------------------------
        // Growth sanity
        // --------------------------------------------------------

        if (
                fd.getExpectedGrowthRate() != null &&
                fd.getExpectedGrowthRate() > 1000
        ) {

            throw new BadRequestException(
                    "Expected annual growth rate is unrealistically high. " +
                    "Please enter a value between 0% and 1000%."
            );
        }


        // --------------------------------------------------------
        // Projection period
        // --------------------------------------------------------

        if (
                fd.getProjectionPeriodYears() != null &&
                (
                        fd.getProjectionPeriodYears() < 1 ||
                        fd.getProjectionPeriodYears() > 10
                )
        ) {

            throw new BadRequestException(
                    "Projection period must be between 1 and 10 years."
            );
        }


        // --------------------------------------------------------
        // Unit economics consistency
        // --------------------------------------------------------

        if (
                fd.getSellingPricePerUnit() != null &&
                fd.getVariableCostPerUnit() != null &&
                fd.getSellingPricePerUnit()
                        .compareTo(
                                fd.getVariableCostPerUnit()
                        ) < 0
        ) {

            throw new BadRequestException(
                    "Selling price per unit cannot be lower than " +
                    "variable cost per unit."
            );
        }
    }


    // ============================================================
    // REQUIRED TEXT
    // ============================================================

    private void requireText(
            String value,
            String message
    ) {

        if (
                value == null ||
                value.trim().isEmpty()
        ) {

            throw new BadRequestException(
                    message
            );
        }
    }


    // ============================================================
    // MEANINGFUL TEXT VALIDATION
    // ============================================================

    private void validateMeaningfulText(
            String value,
            String fieldName
    ) {

        if (
                value == null ||
                value.trim().isEmpty()
        ) {

            throw new BadRequestException(
                    fieldName +
                    " cannot be empty."
            );
        }

        String text =
                value.trim();


        // Minimum length
        if (text.length() < 8) {

            throw new BadRequestException(
                    fieldName +
                    " is too short. Please provide meaningful information."
            );
        }


        // Must contain letters
        if (
                !LETTER_PATTERN
                        .matcher(text)
                        .find()
        ) {

            throw new BadRequestException(
                    fieldName +
                    " must contain meaningful text."
            );
        }


        // Repetitive input
        if (
                isHighlyRepetitive(text)
        ) {

            throw new BadRequestException(
                    fieldName +
                    " appears to contain repetitive or invalid text."
            );
        }


        // Gibberish detection
        if (
                isLikelyGibberish(text)
        ) {

            throw new BadRequestException(
                    fieldName +
                    " appears to contain invalid or meaningless text. " +
                    "Please provide a real business description."
            );
        }


        // Word count
        long meaningfulWords =
                WORD_PATTERN
                        .matcher(text)
                        .results()
                        .count();

        if (
                meaningfulWords < 2
        ) {

            throw new BadRequestException(
                    fieldName +
                    " needs more detail. Please use a short sentence " +
                    "instead of a single word or random characters."
            );
        }
    }


    // ============================================================
    // GIBBERISH DETECTION
    // ============================================================

    private boolean isLikelyGibberish(
            String text
    ) {

        String normalized =
                text
                        .toLowerCase()
                        .replaceAll(
                                "[^a-z\\s]",
                                " "
                        )
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        if (
                normalized.isEmpty()
        ) {
            return true;
        }

        String[] words =
                normalized.split(
                        "\\s+"
                );

        int suspiciousWords = 0;

        int substantialWords = 0;


        for (String word : words) {

            if (
                    word.length() < 4
            ) {
                continue;
            }

            substantialWords++;

            int letters = 0;
            int vowels = 0;
            int consonants = 0;

            int longestConsonantRun = 0;
            int currentConsonantRun = 0;


            for (
                    char c :
                    word.toCharArray()
            ) {

                if (
                        !Character.isLetter(c)
                ) {
                    continue;
                }

                letters++;


                if (
                        isVowel(c)
                ) {

                    vowels++;

                    currentConsonantRun = 0;

                } else {

                    consonants++;

                    currentConsonantRun++;

                    longestConsonantRun =
                            Math.max(
                                    longestConsonantRun,
                                    currentConsonantRun
                            );
                }
            }


            if (
                    letters == 0
            ) {
                continue;
            }


            double consonantRatio =
                    (double) consonants /
                    letters;


            /*
             * Detect words such as:
             *
             * dcvtrht
             * frgyjufd
             * fvfgngn
             */
            if (
                    consonantRatio >= 0.80 &&
                    longestConsonantRun >= 4
            ) {

                suspiciousWords++;
            }


            /*
             * Long words without vowels are suspicious.
             */
            if (
                    word.length() >= 6 &&
                    vowels == 0
            ) {

                suspiciousWords++;
            }
        }


        /*
         * No substantial words at all.
         */
        if (
                substantialWords == 0
        ) {
            return true;
        }


        /*
         * If most substantial words look like gibberish,
         * reject the field.
         */
        return suspiciousWords >=
                Math.max(
                        1,
                        (int) Math.ceil(
                                substantialWords * 0.60
                        )
                );
    }


    // ============================================================
    // REPETITION DETECTION
    // ============================================================

    private boolean isHighlyRepetitive(
            String text
    ) {

        String normalized =
                text.replaceAll(
                        "\\s+",
                        ""
                );

        if (
                normalized.length() < 6
        ) {
            return false;
        }

        int maxSameCharacter = 1;
        int current = 1;


        for (
                int i = 1;
                i < normalized.length();
                i++
        ) {

            if (
                    Character.toLowerCase(
                            normalized.charAt(i)
                    )
                    ==
                    Character.toLowerCase(
                            normalized.charAt(i - 1)
                    )
            ) {

                current++;

                maxSameCharacter =
                        Math.max(
                                maxSameCharacter,
                                current
                        );

            } else {

                current = 1;
            }
        }


        /*
         * Example:
         *
         * aaaaaaaa
         * xxxxxxxx
         */
        return maxSameCharacter >= 6;
    }


    // ============================================================
    // DESCRIPTION QUALITY
    // ============================================================

    private boolean hasMeaningfulDescription(
            String value
    ) {

        if (
                value == null ||
                value.trim().isEmpty()
        ) {
            return false;
        }

        String text =
                value.trim();


        if (
                text.length() < 15
        ) {
            return false;
        }


        if (
                isHighlyRepetitive(text)
        ) {
            return false;
        }


        if (
                isLikelyGibberish(text)
        ) {
            return false;
        }


        long words =
                WORD_PATTERN
                        .matcher(text)
                        .results()
                        .count();

        return words >= 3;
    }


    // ============================================================
    // VOWEL CHECK
    // ============================================================

    private boolean isVowel(
            char c
    ) {

        return c == 'a' ||
                c == 'e' ||
                c == 'i' ||
                c == 'o' ||
                c == 'u';
    }


    // ============================================================
    // FINANCIAL VALIDATION
    // ============================================================

    private void validateNonNegative(
            BigDecimal value,
            String fieldName
    ) {

        if (
                value != null &&
                value.compareTo(
                        BigDecimal.ZERO
                ) < 0
        ) {

            throw new BadRequestException(
                    fieldName +
                    " cannot be negative."
            );
        }
    }


    private void validateNonNegative(
            Integer value,
            String fieldName
    ) {

        if (
                value != null &&
                value < 0
        ) {

            throw new BadRequestException(
                    fieldName +
                    " cannot be negative."
            );
        }
    }


    /*
     * IMPORTANT:
     * expectedGrowthRate is Double in your FinancialData entity.
     */
    private void validateNonNegative(
            Double value,
            String fieldName
    ) {

        if (
                value != null &&
                value < 0
        ) {

            throw new BadRequestException(
                    fieldName +
                    " cannot be negative."
            );
        }
    }


    // ============================================================
    // GET VALIDATION RESULT
    // ============================================================

    @Transactional(readOnly = true)
    public ValidationResultResponse getValidationResult(
            User user,
            Long validationId
    ) {

        Validation v =
                validationRepository
                        .findById(validationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Validation not found."
                                )
                        );


        if (
                !v.getStartup()
                        .getUser()
                        .getId()
                        .equals(
                                user.getId()
                        )
        ) {

            throw new ResourceNotFoundException(
                    "Validation not found."
            );
        }


        System.out.println("=== DATABASE SAVE COMPLETE - BUILDING RESPONSE ==="); return toResultResponse(
                v,
                v.getStartup()
        );
    }


// ============================================================
// VALIDATION HISTORY
// ============================================================

@Transactional(readOnly = true)
public List<ValidationHistoryItemDto> history(User user) {

    return validationRepository
            .findByStartupUserIdOrderByCreatedAtDesc(user.getId())
            .stream()
            .filter(v ->
                    v.getViabilityScore() != null
                            && v.getClassification() != null
                            && v.getRiskLevel() != null
            )
            .map(v ->
                    new ValidationHistoryItemDto(
                            v.getId(),
                            v.getStartup().getId(),
                            v.getStartup().getName(),
                            v.getStartup().getIndustry(),
                            v.getViabilityScore(),
                            v.getClassification().name(),
                            v.getRiskLevel().name(),
                            v.getCreatedAt(),
                            true
                    )
            )
            .toList();
}

    // ============================================================
    // OWNED VALIDATION
    // ============================================================

    @Transactional(readOnly = true)
    Validation getOwnedValidationEntity(
            User user,
            Long validationId
    ) {

        Validation v =
                validationRepository
                        .findById(validationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Validation not found."
                                )
                        );


        if (
                !v.getStartup()
                        .getUser()
                        .getId()
                        .equals(
                                user.getId()
                        )
        ) {

            throw new ResourceNotFoundException(
                    "Validation not found."
            );
        }


        return v;
    }


    // ============================================================
    // RESPONSE
    // ============================================================

    private ValidationResultResponse toResultResponse(
            Validation v,
            Startup startup
    ) {

        ScoreBreakdownDto breakdown =
                new ScoreBreakdownDto(
                        round1(
                                v.getMarketScore()
                        ),

                        round1(
                                v.getBusinessModelScore()
                        ),

                        round1(
                                100 -
                                v.getCompetitionScore()
                        ),

                        round1(
                                v.getFinancialScore()
                        ),

                        round1(
                                v.getScalabilityScore()
                        )
                );


        return new ValidationResultResponse(
                v.getId(),
                startup.getId(),
                startup.getName(),
                round1(
                        v.getViabilityScore()
                ),
                v.getClassification()
                        .display(),
                round1(
                        v.getConfidence()
                ),
                v.getRiskLevel().name(),
                labelFor(
                        v.getMarketScore()
                ),
                labelFor(
                        v.getFinancialScore()
                ),
                labelFor(
                        v.getScalabilityScore()
                ),
                breakdown,
                v.getDetectedIndustry(),
                v.getCreatedAt()
        );
    }


    // ============================================================
    // SCORE LABEL
    // ============================================================

    private String labelFor(
            double score
    ) {

        if (
                score >= 75
        ) {
            return "High";
        }

        if (
                score >= 45
        ) {
            return "Moderate";
        }

        return "Low";
    }


    /**
     * The founder-selected industry is authoritative when supplied. NLP is
     * used for subcategory signals, but delivery/platform keywords must not
     * silently turn an E-commerce startup into Logistics.
     */
    private String contextAwareIndustry(String selectedIndustry, String nlpIndustry) {
        String selected = selectedIndustry == null ? "" : selectedIndustry.trim();
        String detected = nlpIndustry == null ? "" : nlpIndustry.trim();
        return !selected.isBlank() ? selected : (detected.isBlank() ? "Other" : detected);
    }


    // ============================================================
    // MARKET SIZE ESTIMATION
    // ============================================================

    private double estimateMarketSize(
            FinancialData fd,
            NlpExtractionResult nlp
    ) {

        int customers =
                fd.getExpectedMonthlyCustomers() == null
                        ? 0
                        : fd.getExpectedMonthlyCustomers();


        double base =
                40 +
                Math.min(
                        30,
                        Math.log10(
                                customers + 1
                        ) * 12
                );


        double growthBoost =
                fd.getExpectedGrowthRate() == null
                        ? 0
                        : Math.min(
                                20,
                                fd.getExpectedGrowthRate()
                                        * 0.25
                        );


        return Math.max(
                5,
                Math.min(
                        100,
                        base + growthBoost
                )
        );
    }


    // ============================================================
    // COMPETITION LEVEL
    // ============================================================

    private double estimateCompetitionLevel(
            List<Competitor> competitors
    ) {

        int count =
                competitors == null
                        ? 0
                        : competitors.size();


        return Math.max(
                15,
                Math.min(
                        95,
                        25 + count * 15
                )
        );
    }


    // ============================================================
    // FINANCIAL HEALTH
    // ============================================================

    private double scoreFinancialHealth(
            com.ventureai.dto.finance.FinancialSummaryDto summary,
            MlPredictionResult prediction
    ) {

        double marginScore =
                Math.max(
                        0,
                        Math.min(
                                100,
                                50 +
                                summary.profitMarginPercent()
                        )
                );


        double roiScore =
                Math.max(
                        0,
                        Math.min(
                                100,
                                50 +
                                summary.roiPercent() / 2
                        )
                );


        double runwayScore =
                summary.runwayMonths() == null
                        ? 70
                        : Math.max(
                                0,
                                Math.min(
                                        100,
                                        summary.runwayMonths()
                                                * 5
                                )
                        );


        return Math.round(
                (
                        (
                                marginScore +
                                roiScore +
                                runwayScore
                        ) / 3.0
                ) * 10
        ) / 10.0;
    }


    // ============================================================
    // ROUND
    // ============================================================

    private double round1(
            double value
    ) {

        return Math.round(
                value * 10
        ) / 10.0;
    }


    // ============================================================
    // MONTHLY OPERATING COST FALLBACK
    // ============================================================

    private BigDecimal nonZero(
            BigDecimal value,
            FinancialData fd
    ) {

        if (
                value != null &&
                value.doubleValue() > 0
        ) {

            return value;
        }


        double itemized =
                safe(
                        fd.getEmployeeCost()
                )
                +
                safe(
                        fd.getMarketingCost()
                )
                +
                safe(
                        fd.getInfrastructureCost()
                )
                +
                safe(
                        fd.getProductDevelopmentCost()
                );


        return BigDecimal.valueOf(
                itemized > 0
                        ? itemized
                        : 50000
        );
    }


    // ============================================================
    // SAFE NUMBER
    // ============================================================

    private double safe(
            BigDecimal value
    ) {

        return value == null
                ? 0
                : value.doubleValue();
    }


    // ============================================================
    // JSON
    // ============================================================

    private String writeJson(
            Object object
    ) {

        try {

            return objectMapper
                    .writeValueAsString(
                            object
                    );

        } catch (Exception e) {

            return "[]";
        }
    }
}
