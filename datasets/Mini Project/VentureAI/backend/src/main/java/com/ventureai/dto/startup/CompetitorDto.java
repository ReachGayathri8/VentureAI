package com.ventureai.dto.startup;

public record CompetitorDto(
        Long id,
        String name,
        String website,
        String description,
        String pricing,
        String strengths,
        String weaknesses,
        String marketPosition,
        String source,
        Integer priceRating,
        Integer featureRating,
        Integer marketReachRating,
        Integer innovationRating
) {
}
