package com.ventureai.dto.dashboard;

import java.time.LocalDateTime;

public record ViabilityHistoryPointDto(
        String label,
        LocalDateTime date,
        double value
) {
}
