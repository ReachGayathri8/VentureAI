package com.ventureai.dto.ai;

import java.util.List;

public record SwotDto(
        List<String> strengths,
        List<String> weaknesses,
        List<String> opportunities,
        List<String> threats
) {
}
