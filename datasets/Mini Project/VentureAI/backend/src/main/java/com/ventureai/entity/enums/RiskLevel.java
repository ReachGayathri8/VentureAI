package com.ventureai.entity.enums;

public enum RiskLevel {
    LOW, MEDIUM, HIGH;

    public static RiskLevel fromScore(double competitionLevel, double marketVolatility) {
        double avg = (competitionLevel + marketVolatility) / 2.0;
        if (avg >= 66) return HIGH;
        if (avg >= 33) return MEDIUM;
        return LOW;
    }
}
