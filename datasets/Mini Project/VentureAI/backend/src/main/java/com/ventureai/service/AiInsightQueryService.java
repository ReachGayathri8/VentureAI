package com.ventureai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureai.dto.ai.*;
import com.ventureai.dto.startup.CompetitorDto;
import com.ventureai.entity.AiInsight;
import com.ventureai.entity.Competitor;
import com.ventureai.entity.SwotAnalysis;
import com.ventureai.entity.User;
import com.ventureai.entity.Validation;
import com.ventureai.exception.ResourceNotFoundException;
import com.ventureai.repository.AiInsightRepository;
import com.ventureai.repository.CompetitorRepository;
import com.ventureai.repository.SwotAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiInsightQueryService {

    private final SwotAnalysisRepository swotAnalysisRepository;
    private final AiInsightRepository aiInsightRepository;
    private final CompetitorRepository competitorRepository;
    private final ValidationService validationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SwotDto getSwot(User user, Long validationId) {
        Validation v = validationService.getOwnedValidationEntity(user, validationId);
        SwotAnalysis s = swotAnalysisRepository.findByValidation(v)
                .orElseThrow(() -> new ResourceNotFoundException("SWOT analysis not found for this validation."));
        return new SwotDto(readList(s.getStrengths()), readList(s.getWeaknesses()), readList(s.getOpportunities()), readList(s.getThreats()));
    }

    public CompetitorAnalysisResponse getCompetitorAnalysis(User user, Long validationId) {
        Validation v = validationService.getOwnedValidationEntity(user, validationId);
        AiInsight insight = aiInsightRepository.findByValidation(v)
                .orElseThrow(() -> new ResourceNotFoundException("Competitor analysis not found for this validation."));
        List<Competitor> competitors = competitorRepository.findByStartupId(v.getStartup().getId());
        List<CompetitorDto> dtos = competitors.stream().map(c -> new CompetitorDto(
                c.getId(), c.getName(), c.getWebsite(), c.getDescription(), c.getPricing(), c.getStrengths(),
                c.getWeaknesses(), c.getMarketPosition(), c.getSource().name(), c.getPriceRating(),
                c.getFeatureRating(), c.getMarketReachRating(), c.getInnovationRating()
        )).toList();

        return new CompetitorAnalysisResponse(
                dtos, 4, 4, 2, 5,
                insight.getCompetitiveAdvantage(), insight.getMarketGap(), insight.getDifferentiationStrategy()
        );
    }

    public MarketOpportunityResponse getMarketOpportunity(User user, Long validationId) {
        Validation v = validationService.getOwnedValidationEntity(user, validationId);
        AiInsight insight = aiInsightRepository.findByValidation(v)
                .orElseThrow(() -> new ResourceNotFoundException("Market opportunity analysis not found for this validation."));
        return readValue(insight.getMarketOpportunitiesJson(), MarketOpportunityResponse.class);
    }

    public RecommendationsResponse getRecommendations(User user, Long validationId) {
        Validation v = validationService.getOwnedValidationEntity(user, validationId);
        AiInsight insight = aiInsightRepository.findByValidation(v)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendations not found for this validation."));
        return readValue(insight.getRecommendationsJson(), RecommendationsResponse.class);
    }

    public RiskAnalysisResponse getRiskAnalysis(User user, Long validationId) {
        Validation v = validationService.getOwnedValidationEntity(user, validationId);
        AiInsight insight = aiInsightRepository.findByValidation(v)
                .orElseThrow(() -> new ResourceNotFoundException("Risk analysis not found for this validation."));
        return readValue(insight.getRisksJson(), RiskAnalysisResponse.class);
    }

    public String getFinalVerdict(User user, Long validationId) {
        Validation v = validationService.getOwnedValidationEntity(user, validationId);
        return aiInsightRepository.findByValidation(v).map(AiInsight::getFinalVerdict).orElse("");
    }

    private List<String> readList(String json) {
        return readValue(json, new TypeReference<List<String>>() {});
    }

    private <T> T readValue(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Stored AI insight could not be parsed.");
        }
    }

    private <T> T readValue(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Stored AI insight could not be parsed.");
        }
    }
}
