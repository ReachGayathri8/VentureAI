package com.ventureai.service.ai;

import com.ventureai.dto.ai.*;
import com.ventureai.entity.Competitor;
import com.ventureai.entity.Startup;
import com.ventureai.service.ml.NlpExtractionResult;
import com.ventureai.service.ml.MlPredictionResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, input-driven Business Intelligence engine.
 *
 * This class is used when Gemini is disabled or unavailable.
 *
 * Important:
 * - Does not claim that estimated values are real market data.
 * - Does not generate fake market statistics.
 * - Uses startup inputs, NLP scores, ML prediction and supplied
 *   competitor information.
 * - Numeric market indicators are clearly treated as estimates.
 */
@Component
public class RuleBasedAnalysisEngine {

    public AiAnalysisBundle analyze(
            Startup startup,
            List<Competitor> competitors,
            NlpExtractionResult nlp,
            MlPredictionResult rf,
            double marketSize,
            double competitionLevel,
            double growthRate
    ) {

        SwotDto swot =
                buildSwot(startup, nlp, rf, competitionLevel, growthRate);

        String competitiveAdvantage =
                buildCompetitiveAdvantage(startup, nlp, competitors);

        String marketGap =
                buildMarketGap(startup, competitors);

        String differentiation =
                buildDifferentiation(startup, nlp);

        MarketOpportunityResponse marketOpportunity =
                buildMarketOpportunity(
                        startup,
                        nlp,
                        marketSize,
                        growthRate,
                        competitionLevel
                );

        RecommendationsResponse recommendations =
                buildRecommendations(
                        startup,
                        rf,
                        growthRate,
                        competitionLevel
                );

        RiskAnalysisResponse riskAnalysis =
                buildRiskAnalysis(
                        startup,
                        competitionLevel,
                        marketSize,
                        rf
                );

        String verdict = buildVerdict(startup, rf);

        return new AiAnalysisBundle(
                swot,
                competitiveAdvantage,
                marketGap,
                differentiation,
                marketOpportunity,
                recommendations,
                riskAnalysis,
                verdict,
                false
        );
    }

    // ============================================================
    // SWOT ANALYSIS
    // ============================================================

    private SwotDto buildSwot(
            Startup s,
            NlpExtractionResult nlp,
            MlPredictionResult rf,
            double competition,
            double growth
    ) {

        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> opportunities = new ArrayList<>();
        List<String> threats = new ArrayList<>();

        double uvpScore =
                nlp.feature("unique_value_prop_score", 50);

        double scalabilityScore =
                nlp.feature("scalability_signal", 50);

        double businessModelScore =
                nlp.feature("business_model_clarity", 50);

        double problemScore =
                nlp.feature("problem_severity", 50);

        // ---------------- STRENGTHS ----------------

        if (problemScore >= 65) {
            strengths.add(
                    "The startup addresses a significant customer problem: \""
                            + shorten(s.getProblem()) + "\""
            );
        } else {
            strengths.add(
                    "The startup has a clearly defined customer problem: \""
                            + shorten(s.getProblem()) + "\""
            );
        }

        if (uvpScore >= 65) {
            strengths.add(
                    "The value proposition is relatively strong and clearly connected to the proposed solution: \""
                            + shorten(s.getUniqueValueProposition()) + "\""
            );
        } else {
            strengths.add(
                    "The startup has a defined value proposition that can be tested with its target customers."
            );
        }

        if (scalabilityScore >= 65) {
            strengths.add(
                    "The current business model shows good potential to scale beyond the initial target market."
            );
        } else if (scalabilityScore >= 45) {
            strengths.add(
                    "The business has some scalability potential, although expansion may require additional operational capacity."
            );
        }

        strengths.add(
                "The target customer group is explicitly identified as: "
                        + shorten(s.getTargetMarket())
        );

        // ---------------- WEAKNESSES ----------------

        if (rf.score() < 60) {
            weaknesses.add(
                    "The current viability score of "
                            + Math.round(rf.score())
                            + "/100 indicates that important business assumptions still require validation."
            );
        }

        if (businessModelScore < 55) {
            weaknesses.add(
                    "The business model needs clearer definition around how customers are acquired, served and converted into revenue."
            );
        }

        if (uvpScore < 55) {
            weaknesses.add(
                    "The value proposition may not yet be sufficiently differentiated from existing alternatives."
            );
        }

        if (problemScore < 50) {
            weaknesses.add(
                    "The severity of the customer problem appears moderate and should be validated through direct customer interviews or pilots."
            );
        }

        weaknesses.add(
                "As an early-stage venture, the startup has limited proven traction and brand recognition."
        );

        // ---------------- OPPORTUNITIES ----------------

        if (growth >= 20) {
            opportunities.add(
                    "The business can benefit from the expected growth assumption of approximately "
                            + Math.round(growth)
                            + "%, provided that this assumption is supported by customer and market evidence."
            );
        } else {
            opportunities.add(
                    "There is an opportunity to grow by improving customer adoption, retention and distribution rather than relying only on market growth."
            );
        }

        opportunities.add(
                "Expand into closely related customer segments after establishing product-market fit."
        );

        if (nlp.keywords() != null && !nlp.keywords().isEmpty()) {
            opportunities.add(
                    "Potential product or service extensions can be explored around: "
                            + String.join(
                            ", ",
                            nlp.keywords().stream().limit(4).toList()
                    )
            );
        }

        opportunities.add(
                "Partnerships with businesses operating in the startup's "
                        + safe(s.getIndustry())
                        + " value chain could reduce customer acquisition and distribution costs."
        );

        // ---------------- THREATS ----------------

        if (competition >= 70) {
            threats.add(
                    "High competitive intensity may increase customer acquisition costs and reduce pricing power."
            );
        } else if (competition >= 50) {
            threats.add(
                    "Moderate competitive intensity means the startup will need clear differentiation to win customers."
            );
        } else {
            threats.add(
                    "Low currently identified competition does not guarantee an uncontested market; new entrants may appear if demand is proven."
            );
        }

        threats.add(
                "Customer preferences and purchasing behaviour may change, requiring continuous adaptation."
        );

        threats.add(
                "Operating costs, supplier prices or other industry-specific cost pressures could affect profitability."
        );

        return new SwotDto(
                strengths,
                weaknesses,
                opportunities,
                threats
        );
    }

    // ============================================================
    // COMPETITIVE ADVANTAGE
    // ============================================================

    private String buildCompetitiveAdvantage(
            Startup s,
            NlpExtractionResult nlp,
            List<Competitor> competitors
    ) {

        String uvp = shorten(s.getUniqueValueProposition());

        if (competitors == null || competitors.isEmpty()) {
            return "The proposed competitive advantage is based on "
                    + uvp
                    + ". No competitor records were supplied for this validation, so VentureAI cannot verify whether this advantage is unique in the market. Real competitor research is recommended before making a differentiation claim.";
        }

        return "The proposed competitive advantage is "
                + uvp
                + ". "
                + competitors.size()
                + " competitor record(s) were supplied for comparison. The advantage should be validated by comparing customer experience, pricing, distribution, product features and service quality against those competitors.";
    }

    // ============================================================
    // MARKET GAP
    // ============================================================

    private String buildMarketGap(
            Startup s,
            List<Competitor> competitors
    ) {

        if (competitors == null || competitors.isEmpty()) {
            return "No competitor records were supplied. This means VentureAI cannot reliably confirm a market gap. The proposed gap is the unmet need described as \""
                    + shorten(s.getProblem())
                    + "\" for "
                    + safe(s.getTargetMarket())
                    + ".";
        }

        return "The potential market gap is the ability to solve \""
                + shorten(s.getProblem())
                + "\" for "
                + safe(s.getTargetMarket())
                + ". However, this gap should be confirmed through direct comparison with the "
                + competitors.size()
                + " supplied competitor record(s) rather than assuming that competitors do not address the problem.";
    }

    // ============================================================
    // DIFFERENTIATION
    // ============================================================

    private String buildDifferentiation(
            Startup s,
            NlpExtractionResult nlp
    ) {

        double uvpScore =
                nlp.feature("unique_value_prop_score", 50);

        String strength;

        if (uvpScore >= 70) {
            strength = "strong";
        } else if (uvpScore >= 50) {
            strength = "moderately differentiated";
        } else {
            strength = "not yet strongly differentiated";
        }

        return "The startup should position itself around \""
                + shorten(s.getUniqueValueProposition())
                + "\". NLP analysis indicates that the current value proposition is "
                + strength
                + ". The "
                + safe(s.getRevenueModel())
                + " revenue model should support this positioning without relying only on lower pricing.";
    }

    // ============================================================
    // MARKET OPPORTUNITY
    // ============================================================

    private MarketOpportunityResponse buildMarketOpportunity(
            Startup s,
            NlpExtractionResult nlp,
            double marketSize,
            double growth,
            double competition
    ) {

        /*
         * IMPORTANT:
         *
         * marketSize is the existing VentureAI model estimate.
         * It is NOT treated as an actual market-size value.
         *
         * Therefore we use it as a MARKET ATTRACTIVENESS INDICATOR,
         * not as "TAM" or actual market revenue.
         */

        double problemScore =
                clamp(nlp.feature("problem_severity", 50));

        double uvpScore =
                clamp(nlp.feature("unique_value_prop_score", 50));

        double businessModelScore =
                clamp(nlp.feature("business_model_clarity", 50));

        double scalabilityScore =
                clamp(nlp.feature("scalability_signal", 50));

        double growthScore =
                growthIndicator(growth);

        double competitionAttractiveness =
                100 - clamp(competition);

        /*
         * Demand score:
         *
         * Based on problem severity, UVP and business-model clarity.
         * This avoids treating customer count as actual market demand.
         */
        double demandScore =
                (problemScore * 0.45)
                        + (uvpScore * 0.30)
                        + (businessModelScore * 0.25);

        /*
         * Market opportunity score:
         *
         * Demand       = 35%
         * Growth       = 25%
         * Market fit   = 20%
         * Competition = 20%
         */
        double marketFit =
                (businessModelScore * 0.50)
                        + (scalabilityScore * 0.30)
                        + (uvpScore * 0.20);

        double opportunityScore =
                (demandScore * 0.35)
                        + (growthScore * 0.25)
                        + (marketFit * 0.20)
                        + (competitionAttractiveness * 0.20);

        opportunityScore =
                Math.round(clamp(opportunityScore) * 10.0) / 10.0;

        demandScore =
                Math.round(clamp(demandScore) * 10.0) / 10.0;

        /*
         * Existing marketSize input is retained for DTO compatibility,
         * but is clearly labelled as an estimated market indicator.
         */
        double marketIndicator =
                Math.round(clamp(marketSize) * 10.0) / 10.0;

        String demandLabel;

        if (demandScore >= 70) {
            demandLabel = "Strong customer-demand indicators";
        } else if (demandScore >= 50) {
            demandLabel = "Moderate customer-demand indicators";
        } else {
            demandLabel = "Demand requires further validation";
        }

        String growthLabel;

        if (growth >= 20) {
            growthLabel =
                    "Business growth assumption is relatively strong";
        } else if (growth >= 10) {
            growthLabel =
                    "Business growth assumption is moderate";
        } else {
            growthLabel =
                    "Business growth assumption is conservative";
        }

        // ------------------------------------------------------------
        // CUSTOMER SEGMENTS
        // ------------------------------------------------------------

        List<String> segments =
                buildCustomerSegments(s);

        // ------------------------------------------------------------
        // MARKET TRENDS
        // ------------------------------------------------------------

        List<String> trends =
                buildMarketTrends(s);

        // ------------------------------------------------------------
        // OPPORTUNITY AREAS
        // ------------------------------------------------------------

        List<String> opportunityAreas =
                buildOpportunityAreas(s);

        // ------------------------------------------------------------
        // MARKET RISKS
        // ------------------------------------------------------------

        List<String> risks =
                buildMarketRisks(s, competition);

        /*
         * The DTO currently has no dedicated "market opportunity score"
         * field, so the existing marketGrowthScore field is used to expose
         * the model's opportunity estimate.
         *
         * This preserves frontend compatibility.
         */
        return new MarketOpportunityResponse(
                safe(s.getTargetMarket()),
                demandLabel
                        + " (VentureAI estimate: "
                        + Math.round(demandScore)
                        + "/100)",
                growthLabel
                        + " (business assumption: "
                        + Math.round(growth)
                        + "%)",
                segments,
                trends,
                opportunityAreas,
                risks,
                opportunityScore,
                marketIndicator,
                clamp(competition),
                true
        );
    }

    // ============================================================
    // CUSTOMER SEGMENTS
    // ============================================================

    private List<String> buildCustomerSegments(Startup s) {

        List<String> segments =
                new ArrayList<>();

        String targetMarket =
                safe(s.getTargetMarket());

        segments.add(
                "Primary target: " + shorten(targetMarket)
        );

        /*
         * Do not manufacture fake customer categories.
         * The target-market field supplied by the founder is the
         * primary source.
         */

        String businessModel =
                safe(s.getBusinessModel()).toLowerCase();

        if (businessModel.contains("b2b")
                || businessModel.contains("enterprise")
                || businessModel.contains("business")) {

            segments.add(
                    "Business customers within the stated target market"
            );
        }

        if (businessModel.contains("b2c")
                || businessModel.contains("consumer")
                || businessModel.contains("e-commerce")
                || businessModel.contains("ecommerce")) {

            segments.add(
                    "Individual consumers within the stated target market"
            );
        }

        if (businessModel.contains("marketplace")
                || businessModel.contains("aggregator")) {

            segments.add(
                    "Both supply-side and demand-side participants"
            );
        }

        return segments;
    }

    // ============================================================
    // MARKET TRENDS
    // ============================================================

    private List<String> buildMarketTrends(Startup s) {

        List<String> trends =
                new ArrayList<>();

        String industry =
                safe(s.getIndustry());

        String businessModel =
                safe(s.getBusinessModel()).toLowerCase();

        /*
         * These are directional trends, not numeric market facts.
         */

        if (businessModel.contains("e-commerce")
                || businessModel.contains("ecommerce")
                || businessModel.contains("marketplace")
                || businessModel.contains("on-demand")) {

            trends.add(
                    "Increasing customer preference for convenient digital purchasing and ordering."
            );
        }

        if (businessModel.contains("subscription")
                || businessModel.contains("membership")) {

            trends.add(
                    "Recurring purchasing and subscription models can improve customer retention when the product naturally supports repeat usage."
            );
        }

        if (businessModel.contains("saas")
                || businessModel.contains("software")
                || businessModel.contains("technology")) {

            trends.add(
                    "Digital workflows and software-led service delivery can reduce manual operating effort as adoption increases."
            );
        }

        /*
         * Always retain an industry-specific trend rather than claiming
         * that every industry has the same trend.
         */
        trends.add(
                "Customer expectations, pricing and competitive behaviour within "
                        + industry
                        + " should be monitored continuously."
        );

        return trends;
    }

    // ============================================================
    // OPPORTUNITY AREAS
    // ============================================================

    private List<String> buildOpportunityAreas(Startup s) {

        List<String> opportunities =
                new ArrayList<>();

        opportunities.add(
                "Improve penetration within the existing target market before expanding geographically."
        );

        opportunities.add(
                "Test adjacent customer segments that have similar needs to the current target audience."
        );

        opportunities.add(
                "Develop partnerships relevant to the "
                        + safe(s.getIndustry())
                        + " value chain."
        );

        opportunities.add(
                "Use customer feedback and purchasing behaviour to identify additional products or services around: "
                        + shorten(s.getSolution())
        );

        return opportunities;
    }

    // ============================================================
    // MARKET RISKS
    // ============================================================

    private List<String> buildMarketRisks(
            Startup s,
            double competition
    ) {

        List<String> risks =
                new ArrayList<>();

        if (competition >= 70) {
            risks.add(
                    "High competitive intensity may increase customer acquisition costs and pricing pressure."
            );
        } else if (competition >= 50) {
            risks.add(
                    "Moderate competition requires clear differentiation and efficient customer acquisition."
            );
        } else {
            risks.add(
                    "Competition appears limited in the supplied data, but the competitor dataset may be incomplete."
            );
        }

        risks.add(
                "Demand may vary by customer behaviour, seasonality or economic conditions within "
                        + safe(s.getIndustry())
                        + "."
        );

        risks.add(
                "The startup may need to invest in customer education and trust-building before achieving repeat purchases."
        );

        return risks;
    }

    // ============================================================
    // RECOMMENDATIONS
    // ============================================================

    private RecommendationsResponse buildRecommendations(
            Startup s,
            MlPredictionResult rf,
            double growth,
            double competition
    ) {

        List<String> business =
                new ArrayList<>();

        if (rf.score() < 60) {
            business.add(
                    "Run a controlled pilot with customers from "
                            + safe(s.getTargetMarket())
                            + " before committing significant resources."
            );
        } else {
            business.add(
                    "Scale gradually after validating customer retention, unit economics and repeat demand."
            );
        }

        business.add(
                "Track actual customer acquisition, conversion, retention and profitability instead of relying only on projected growth."
        );

        List<String> marketing =
                List.of(
                        "Lead customer communication with the specific benefit: \""
                                + shorten(s.getUniqueValueProposition())
                                + "\"",
                        "Prioritize acquisition channels where the stated target customers already spend time or make purchasing decisions.",
                        "Use customer testimonials, pilot results and measurable outcomes as social proof."
                );

        List<String> pricing =
                List.of(
                        "Validate willingness-to-pay with real customers before finalizing long-term pricing.",
                        "Compare the proposed price with direct alternatives and the customer's perceived value.",
                        "Monitor contribution margin rather than optimizing only for sales volume."
                );

        List<String> revenueModel =
                List.of(
                        "Validate whether the current "
                                + safe(s.getRevenueModel())
                                + " model produces sustainable margins.",
                        "Consider complementary revenue streams only when they fit the same customer need and do not unnecessarily increase operational complexity."
                );

        List<String> product =
                List.of(
                        "Prioritize the product or service capability that directly solves: "
                                + shorten(s.getProblem()),
                        "Collect structured customer feedback and usage data from the first customers.",
                        "Improve the solution based on measurable customer outcomes rather than assumptions."
                );

        List<String> growthStrategy =
                new ArrayList<>();

        if (growth >= 20) {
            growthStrategy.add(
                    "Treat the "
                            + Math.round(growth)
                            + "% growth figure as a business assumption and validate it against actual market and customer data."
            );
        } else {
            growthStrategy.add(
                    "Use conservative growth assumptions until customer demand and retention are demonstrated."
            );
        }

        growthStrategy.add(
                "Expand geographically or into adjacent segments only after achieving repeatable unit economics."
        );

        List<String> riskMitigation =
                new ArrayList<>();

        if (competition >= 60) {
            riskMitigation.add(
                    "Strengthen differentiation around "
                            + shorten(s.getUniqueValueProposition())
                            + " rather than competing only on price."
            );
        } else {
            riskMitigation.add(
                    "Continue monitoring competitors because low observed competition may reflect incomplete competitor data."
            );
        }

        riskMitigation.add(
                "Maintain sufficient cash runway and review fixed and variable costs regularly."
        );

        List<String> funding =
                new ArrayList<>();

        if (rf.score() >= 70) {
            funding.add(
                    "The current validation indicates relatively strong fundamentals, but funding decisions should also consider traction, unit economics and founder execution capability."
            );
        } else {
            funding.add(
                    "Prioritize validating the business model and customer demand before raising significant external capital."
            );
        }

        funding.add(
                "Maintain a data room containing customer evidence, financial assumptions, competitor research and validation results."
        );

        String classification =
                rf.classification()
                        .toLowerCase()
                        .replace('_', ' ');

        String summary =
                "The startup currently shows "
                        + classification
                        + " based on the VentureAI model. "
                        + "The next priority should be validating the assumptions behind demand, differentiation, customer acquisition and unit economics with real-world evidence.";

        return new RecommendationsResponse(
                business,
                marketing,
                pricing,
                revenueModel,
                product,
                growthStrategy,
                riskMitigation,
                funding,
                summary
        );
    }

    // ============================================================
    // RISK ANALYSIS
    // ============================================================

    private RiskAnalysisResponse buildRiskAnalysis(
            Startup s,
            double competition,
            double marketSize,
            MlPredictionResult rf
    ) {

        List<RiskCategoryDto> categories =
                new ArrayList<>();

        /*
         * Market risk.
         *
         * marketSize is treated as an estimated attractiveness indicator,
         * not as actual market size.
         */
        double marketRisk =
                100 - clamp(marketSize);

        categories.add(
                new RiskCategoryDto(
                        "Market Risk",
                        levelFor(marketRisk),
                        impactFor(marketRisk),
                        "Validate customer demand through interviews, pilots and actual purchasing behaviour in "
                                + safe(s.getTargetMarket())
                )
        );

        double financialRisk =
                rf.score() < 60 ? 65 : 40;

        categories.add(
                new RiskCategoryDto(
                        "Financial Risk",
                        levelFor(financialRisk),
                        impactFor(financialRisk),
                        "Monitor burn rate, contribution margin, cash runway and actual revenue against projections."
                )
        );

        categories.add(
                new RiskCategoryDto(
                        "Competition Risk",
                        levelFor(competition),
                        impactFor(competition),
                        "Compare pricing, customer experience, distribution and product/service quality against real competitors."
                )
        );

        /*
         * Technology risk is kept moderate rather than pretending that
         * every startup has the same technology risk.
         */
        double technologyRisk =
                isTechnologyHeavy(s) ? 55 : 30;

        categories.add(
                new RiskCategoryDto(
                        "Technology Risk",
                        levelFor(technologyRisk),
                        impactFor(technologyRisk),
                        isTechnologyHeavy(s)
                                ? "Validate system reliability, security, scalability and maintenance requirements before rapid expansion."
                                : "Use reliable technology where required without introducing unnecessary technical complexity."
                )
        );

        categories.add(
                new RiskCategoryDto(
                        "Operational Risk",
                        levelFor(45),
                        impactFor(45),
                        "Document important operational processes and identify dependencies on suppliers, partners and key personnel."
                )
        );

        double customerRisk =
                rf.score() < 60 ? 60 : 40;

        categories.add(
                new RiskCategoryDto(
                        "Customer Risk",
                        levelFor(customerRisk),
                        impactFor(customerRisk),
                        "Measure conversion, repeat usage, retention and customer satisfaction to identify demand or churn problems early."
                )
        );

        double overall =
                categories.stream()
                        .mapToDouble(c -> scoreFor(c.riskLevel()))
                        .average()
                        .orElse(50);

        String overallLevel =
                overall >= 66
                        ? "HIGH"
                        : overall >= 40
                        ? "MEDIUM"
                        : "LOW";

        return new RiskAnalysisResponse(
                categories,
                Math.round(overall * 10) / 10.0,
                overallLevel
        );
    }

    // ============================================================
    // VERDICT
    // ============================================================

    private String buildVerdict(
            Startup s,
            MlPredictionResult rf
    ) {

        return switch (rf.classification()) {

            case "HIGH_POTENTIAL" ->
                    s.getName()
                            + " shows strong potential under the VentureAI model. "
                            + "The next priority is proving customer demand, unit economics and competitive positioning with real-world evidence.";

            case "MODERATE_POTENTIAL" ->
                    s.getName()
                            + " shows moderate potential. "
                            + "Improving the business model, customer validation and differentiation could materially strengthen the opportunity.";

            case "NEEDS_IMPROVEMENT" ->
                    s.getName()
                            + " needs improvement in several key areas before it can be considered strongly validated. "
                            + "Focus on demand validation, pricing, differentiation and financial sustainability.";

            default ->
                    s.getName()
                            + " currently carries high modeled risk. "
                            + "Treat the idea as an early hypothesis and validate the core customer, market and financial assumptions before committing significant capital.";
        };
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private double growthIndicator(double growth) {

        /*
         * Transparent directional conversion of the supplied growth
         * assumption into a 0-100 indicator.
         *
         * This is NOT a market CAGR.
         */
        if (growth <= 0) {
            return 20;
        }

        if (growth <= 5) {
            return 30;
        }

        if (growth <= 10) {
            return 45;
        }

        if (growth <= 20) {
            return 60;
        }

        if (growth <= 30) {
            return 72;
        }

        if (growth <= 50) {
            return 82;
        }

        if (growth <= 75) {
            return 90;
        }

        return 95;
    }

    private boolean isTechnologyHeavy(Startup s) {

        String industry =
                safe(s.getIndustry()).toLowerCase();

        String businessModel =
                safe(s.getBusinessModel()).toLowerCase();

        return industry.contains("technology")
                || industry.contains("software")
                || industry.contains("ai")
                || industry.contains("machine learning")
                || industry.contains("saas")
                || industry.contains("cyber")
                || industry.contains("fintech")
                || businessModel.contains("saas")
                || businessModel.contains("software");
    }

    private String levelFor(double value) {

        return value >= 66
                ? "HIGH"
                : value >= 33
                ? "MEDIUM"
                : "LOW";
    }

    private String impactFor(double value) {

        return value >= 66
                ? "HIGH"
                : value >= 33
                ? "MEDIUM"
                : "LOW";
    }

    private double scoreFor(String level) {

        return switch (level) {
            case "HIGH" -> 80;
            case "MEDIUM" -> 50;
            default -> 20;
        };
    }

    private double clamp(double value) {

        return Math.max(
                0,
                Math.min(100, value)
        );
    }

    private String safe(String value) {

        return (value == null || value.isBlank())
                ? "the target market"
                : value.trim();
    }

    private String shorten(String value) {

        if (value == null || value.isBlank()) {
            return "the stated problem";
        }

        String cleaned =
                value.trim();

        return cleaned.length() > 120
                ? cleaned.substring(0, 120) + "..."
                : cleaned;
    }
}
