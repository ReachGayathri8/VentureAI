package com.ventureai.entity.enums;

public enum Classification {
    HIGH_POTENTIAL,
    MODERATE_POTENTIAL,
    NEEDS_IMPROVEMENT,
    HIGH_RISK;

    public static Classification fromScore(double score) {
        if (score >= 80) return HIGH_POTENTIAL;
        if (score >= 60) return MODERATE_POTENTIAL;
        if (score >= 40) return NEEDS_IMPROVEMENT;
        return HIGH_RISK;
    }

    public String display() {
        return switch (this) {
            case HIGH_POTENTIAL -> "High Potential";
            case MODERATE_POTENTIAL -> "Moderate Potential";
            case NEEDS_IMPROVEMENT -> "Needs Improvement";
            case HIGH_RISK -> "High Risk";
        };
    }
}
