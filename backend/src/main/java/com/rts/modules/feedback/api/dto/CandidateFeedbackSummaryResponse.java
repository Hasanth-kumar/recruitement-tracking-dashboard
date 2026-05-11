package com.rts.modules.feedback.api.dto;

import java.util.List;

public record CandidateFeedbackSummaryResponse(
        String candidateId,
        int totalFeedbackCount,
        Double averageTechnicalRating,
        Double averageCommunicationRating,
        Double averageProblemSolvingRating,
        Double averageLeadershipRating,
        Double averageCultureRating,
        Double overallAverageRating,
        List<FeedbackResponse> feedbacks
) {

    public static CandidateFeedbackSummaryResponse from(String candidateId, List<FeedbackResponse> feedbacks) {
        if (feedbacks.isEmpty()) {
            return new CandidateFeedbackSummaryResponse(
                    candidateId, 0, null, null, null, null, null, null, feedbacks
            );
        }

        double avgTech = feedbacks.stream().mapToInt(FeedbackResponse::technicalRating).average().orElse(0);
        double avgComm = feedbacks.stream().mapToInt(FeedbackResponse::communicationRating).average().orElse(0);
        double avgProb = feedbacks.stream().mapToInt(FeedbackResponse::problemSolvingRating).average().orElse(0);
        double avgLead = feedbacks.stream().mapToInt(FeedbackResponse::leadershipRating).average().orElse(0);
        double avgCult = feedbacks.stream().mapToInt(FeedbackResponse::cultureRating).average().orElse(0);
        double overall = (avgTech + avgComm + avgProb + avgLead + avgCult) / 5.0;

        return new CandidateFeedbackSummaryResponse(
                candidateId,
                feedbacks.size(),
                round(avgTech),
                round(avgComm),
                round(avgProb),
                round(avgLead),
                round(avgCult),
                round(overall),
                feedbacks
        );
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
