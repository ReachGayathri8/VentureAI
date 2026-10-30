package com.ventureai.dto.validation;

import java.time.LocalDateTime;

public record ValidationHistoryItemDto(

        Long validationId,

        Long startupId,

        String startupName,

        String industry,

        Double viabilityScore,

        String classification,

        String riskLevel,

        LocalDateTime createdAt,

        boolean reportAvailable

) {
}
