package com.ventureai.dto.ai;

import java.util.List;

public record RecommendationsResponse(
        List<String> business,
        List<String> marketing,
        List<String> pricing,
        List<String> revenueModel,
        List<String> product,
        List<String> growthStrategy,
        List<String> riskMitigation,
        List<String> funding,
        String summary
) {
}
