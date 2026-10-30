package com.ventureai.controller;

import com.ventureai.dto.ai.*;
import com.ventureai.dto.validation.ValidationHistoryItemDto;
import com.ventureai.dto.validation.ValidationResultResponse;
import com.ventureai.exception.BadRequestException;
import com.ventureai.repository.ValidationRepository;
import com.ventureai.service.AiInsightQueryService;
import com.ventureai.service.StartupService;
import com.ventureai.service.ValidationService;
import com.ventureai.util.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ValidationController {

    private final ValidationService validationService;
    private final AiInsightQueryService aiInsightQueryService;
    private final CurrentUserProvider currentUserProvider;

    private final ValidationRepository validationRepository;
    private final StartupService startupService;


    // ============================================================
    // RUN VALIDATION
    // ============================================================

    @PostMapping("/validation/{startupId}")
    public ResponseEntity<ValidationResultResponse> runValidation(
            @PathVariable Long startupId
    ) {

        var user =
                currentUserProvider.getCurrentUser();

        /*
         * Determine whether this startup already has
         * at least one successful validation.
         *
         * If it does not, an invalid first attempt can be
         * safely removed after the validation gate rejects it.
         */
        boolean hadPreviousValidation =
                validationRepository
                        .existsByStartupId(
                                startupId
                        );

        try {

            ValidationResultResponse result =
                    validationService.runValidation(
                            user,
                            startupId
                    );

            return ResponseEntity.ok(
                    result
            );

        } catch (
                BadRequestException ex
        ) {

            /*
             * If this was a brand-new startup and validation
             * failed because the input was invalid/incomplete,
             * remove the temporary startup.
             *
             * This prevents junk records such as:
             *
             * acd
             * dcvtrht
             * frgyjufd
             *
             * from appearing in My Startups.
             */
            if (!hadPreviousValidation) {

                try {

                    startupService.deleteStartup(
                            user,
                            startupId
                    );

                } catch (Exception ignored) {

                    /*
                     * Do not hide the original validation error
                     * if cleanup itself encounters a problem.
                     */
                }
            }

            throw ex;
        }
    }


    // ============================================================
    // GET VALIDATION RESULT
    // ============================================================

    @GetMapping("/validation/{id}")
    public ResponseEntity<ValidationResultResponse> getResult(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                validationService.getValidationResult(
                        currentUserProvider.getCurrentUser(),
                        id
                )
        );
    }


    // ============================================================
    // SWOT
    // ============================================================

    @GetMapping("/validation/{id}/swot")
    public ResponseEntity<SwotDto> getSwot(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                aiInsightQueryService.getSwot(
                        currentUserProvider.getCurrentUser(),
                        id
                )
        );
    }


    // ============================================================
    // COMPETITORS
    // ============================================================

    @GetMapping("/validation/{id}/competitors")
    public ResponseEntity<CompetitorAnalysisResponse> getCompetitors(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                aiInsightQueryService.getCompetitorAnalysis(
                        currentUserProvider.getCurrentUser(),
                        id
                )
        );
    }


    // ============================================================
    // MARKET OPPORTUNITY
    // ============================================================

    @GetMapping("/validation/{id}/market-opportunity")
    public ResponseEntity<MarketOpportunityResponse>
    getMarketOpportunity(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                aiInsightQueryService.getMarketOpportunity(
                        currentUserProvider.getCurrentUser(),
                        id
                )
        );
    }


    // ============================================================
    // RECOMMENDATIONS
    // ============================================================

    @GetMapping("/validation/{id}/recommendations")
    public ResponseEntity<RecommendationsResponse>
    getRecommendations(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                aiInsightQueryService.getRecommendations(
                        currentUserProvider.getCurrentUser(),
                        id
                )
        );
    }


    // ============================================================
    // RISKS
    // ============================================================

    @GetMapping("/validation/{id}/risks")
    public ResponseEntity<RiskAnalysisResponse>
    getRisks(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                aiInsightQueryService.getRiskAnalysis(
                        currentUserProvider.getCurrentUser(),
                        id
                )
        );
    }


    // ============================================================
    // VERDICT
    // ============================================================

    @GetMapping("/validation/{id}/verdict")
    public ResponseEntity<Map<String, String>>
    getVerdict(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                Map.of(
                        "verdict",
                        aiInsightQueryService.getFinalVerdict(
                                currentUserProvider.getCurrentUser(),
                                id
                        )
                )
        );
    }


    // ============================================================
    // HISTORY
    // ============================================================

    @GetMapping("/validation-history")
    public ResponseEntity<List<ValidationHistoryItemDto>>
    history() {

        return ResponseEntity.ok(
                validationService.history(
                        currentUserProvider.getCurrentUser()
                )
        );
    }
}
