package com.rts.modules.feedback.api.dto;

import com.rts.modules.feedback.domain.Feedback;
import com.rts.modules.feedback.domain.FeedbackRecommendation;

import java.time.LocalDateTime;

public record FeedbackResponse(
        String id,
        String interviewId,
        String candidateId,
        String submittedByUsername,
        int technicalRating,
        int communicationRating,
        int problemSolvingRating,
        int leadershipRating,
        int cultureRating,
        FeedbackRecommendation recommendation,
        String comments,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FeedbackResponse from(Feedback f) {
        return new FeedbackResponse(
                f.getId(),
                f.getInterviewId(),
                f.getCandidateId(),
                f.getSubmittedByUsername(),
                f.getTechnicalRating(),
                f.getCommunicationRating(),
                f.getProblemSolvingRating(),
                f.getLeadershipRating(),
                f.getCultureRating(),
                f.getRecommendation(),
                f.getComments() == null ? "" : f.getComments(),
                f.getSubmittedAt(),
                f.getCreatedAt(),
                f.getUpdatedAt()
        );
    }
}
