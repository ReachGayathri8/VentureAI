package com.ventureai.dto.validation;

public record ScoreBreakdownDto(
        double marketPotential,
        double businessModel,
        double competition,
        double financialFeasibility,
        double scalability
) {
}
