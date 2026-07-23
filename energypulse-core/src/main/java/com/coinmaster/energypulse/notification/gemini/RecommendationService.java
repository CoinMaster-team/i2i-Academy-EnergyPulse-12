package com.coinmaster.energypulse.notification.gemini;

import com.coinmaster.energypulse.notification.dto.GeneratedRecommendation;
import com.coinmaster.energypulse.notification.dto.NotificationCandidate;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final RecommendationClient recommendationClient;
    private final NotificationPromptService promptService;
    private final FallbackRecommendationService fallbackService;

    public RecommendationService(
            RecommendationClient recommendationClient,
            NotificationPromptService promptService,
            FallbackRecommendationService fallbackService) {
        this.recommendationClient = recommendationClient;
        this.promptService = promptService;
        this.fallbackService = fallbackService;
    }

    public String createPrompt(NotificationCandidate candidate) {
        return promptService.createPrompt(candidate);
    }

    public GeneratedRecommendation generate(
            NotificationCandidate candidate,
            String prompt) {
        try {
            return GeneratedRecommendation.generated(
                    recommendationClient.generateRecommendation(prompt));
        } catch (RuntimeException exception) {
            return GeneratedRecommendation.fallback(
                    fallbackService.createFallback(candidate),
                    shortenError(exception));
        }
    }

    private String shortenError(RuntimeException exception) {
        String message = exception.getMessage();
        String safeMessage = message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;

        return safeMessage.substring(0, Math.min(safeMessage.length(), MAX_ERROR_LENGTH));
    }
}
