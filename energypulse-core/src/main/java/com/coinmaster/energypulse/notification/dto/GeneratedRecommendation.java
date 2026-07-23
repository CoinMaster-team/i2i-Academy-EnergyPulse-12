package com.coinmaster.energypulse.notification.dto;

public record GeneratedRecommendation(
        String text,
        String status,
        String error) {

    public static GeneratedRecommendation generated(String text) {
        return new GeneratedRecommendation(text, "GENERATED", null);
    }

    public static GeneratedRecommendation fallback(String text, String error) {
        return new GeneratedRecommendation(text, "FALLBACK", error);
    }
}
