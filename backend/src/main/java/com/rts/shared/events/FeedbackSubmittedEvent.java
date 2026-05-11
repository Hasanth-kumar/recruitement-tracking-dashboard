package com.rts.shared.events;

import com.rts.modules.feedback.domain.FeedbackRecommendation;

import java.time.LocalDateTime;

public record FeedbackSubmittedEvent(
        String feedbackId,
        String interviewId,
        String candidateId,
        String submittedByUsername,
        FeedbackRecommendation recommendation,
        LocalDateTime submittedAt
) {
}
