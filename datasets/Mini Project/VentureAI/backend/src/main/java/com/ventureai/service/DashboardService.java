package com.ventureai.service;
import org.springframework.transaction.annotation.Transactional;
import com.ventureai.dto.dashboard.DashboardResponse;
import com.ventureai.dto.dashboard.ViabilityHistoryPointDto;
import com.ventureai.dto.validation.ValidationHistoryItemDto;
import com.ventureai.entity.User;
import com.ventureai.entity.Validation;
import com.ventureai.entity.enums.Classification;
import com.ventureai.repository.ReportRepository;
import com.ventureai.repository.ValidationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ValidationRepository validationRepository;
    private final ReportRepository reportRepository;

    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM d");
    @Transactional (readOnly = true)
    public DashboardResponse getDashboard(User user) {
        List<Validation> validations = validationRepository.findByStartupUserIdOrderByCreatedAtDesc(user.getId());

        long total = validations.size();
        double avgScore = validations.stream().mapToDouble(Validation::getViabilityScore).average().orElse(0);
        long highPotential = validations.stream().filter(v -> v.getClassification() == Classification.HIGH_POTENTIAL).count();
        long reportsGenerated = validations.stream()
                .filter(v -> reportRepository.findByValidationId(v.getId()).isPresent())
                .count();

        List<ValidationHistoryItemDto> recent = validations.stream().limit(8).map(v -> new ValidationHistoryItemDto(
                v.getId(), v.getStartup().getId(), v.getStartup().getName(), v.getStartup().getIndustry(),
                v.getViabilityScore(), v.getClassification().name(), v.getRiskLevel().name(),
                v.getCreatedAt(), reportRepository.findByValidationId(v.getId()).isPresent()
        )).toList();

        List<ViabilityHistoryPointDto> scoreHistory = validations.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(v -> new ViabilityHistoryPointDto(v.getCreatedAt().format(LABEL_FORMAT), v.getCreatedAt(), v.getViabilityScore()))
                .toList();

        Map<String, Long> industryDistribution = new LinkedHashMap<>();
        validations.forEach(v -> industryDistribution.merge(
                v.getStartup().getIndustry() == null ? "Other" : v.getStartup().getIndustry(), 1L, Long::sum));

        List<ViabilityHistoryPointDto> marketTrend = validations.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(v -> new ViabilityHistoryPointDto(v.getCreatedAt().format(LABEL_FORMAT), v.getCreatedAt(), v.getMarketScore()))
                .toList();

        List<ViabilityHistoryPointDto> financialTrend = validations.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(v -> new ViabilityHistoryPointDto(v.getCreatedAt().format(LABEL_FORMAT), v.getCreatedAt(), v.getFinancialScore()))
                .toList();

        return new DashboardResponse(total, Math.round(avgScore * 10) / 10.0, highPotential, reportsGenerated,
                recent, scoreHistory, industryDistribution, marketTrend, financialTrend);
    }
}
