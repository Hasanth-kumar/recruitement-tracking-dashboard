package com.rts.modules.feedback.api.dto;

import com.rts.modules.feedback.domain.FeedbackRecommendation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Structured interview feedback (five 1–5 ratings, recommendation, optional comments)")
public record SubmitFeedbackRequest(
        @NotBlank
        @Schema(description = "Interview this feedback applies to", example = "550e8400-e29b-41d4-a716-446655440000")
        String interviewId,

        @NotNull
        @Min(1)
        @Max(5)
        @Schema(description = "Technical competency (1–5)", example = "4")
        Integer technicalRating,

        @NotNull
        @Min(1)
        @Max(5)
        @Schema(description = "Communication (1–5)", example = "3")
        Integer communicationRating,

        @NotNull
        @Min(1)
        @Max(5)
        @Schema(description = "Problem solving (1–5)", example = "4")
        Integer problemSolvingRating,

        @NotNull
        @Min(1)
        @Max(5)
        @Schema(description = "Leadership potential (1–5)", example = "3")
        Integer leadershipRating,

        @NotNull
        @Min(1)
        @Max(5)
        @Schema(description = "Culture fit (1–5)", example = "4")
        Integer cultureRating,

        @NotNull
        FeedbackRecommendation recommendation,

        @Size(max = 1000)
        @Schema(description = "Optional free-text comments (max 1000 characters)")
        String comments
) {
}
