package com.rts.modules.feedback.api;

import com.rts.modules.feedback.api.dto.CandidateFeedbackSummaryResponse;
import com.rts.modules.feedback.api.dto.FeedbackResponse;
import com.rts.modules.feedback.api.dto.SubmitFeedbackRequest;
import com.rts.modules.feedback.application.FeedbackService;
import com.rts.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Feedback")
@RestController
@RequestMapping("/api")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @Operation(
            summary = "Submit or update interview feedback",
            description = "Creates feedback or updates the caller's existing row within 24 hours of first submission. "
                    + "Updates the candidate evaluation score as the average of per-feedback mean ratings (1–5 scale)."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER', 'INTERVIEWER')")
    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<FeedbackResponse>> submit(@Valid @RequestBody SubmitFeedbackRequest request) {
        FeedbackResponse body = feedbackService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Feedback submitted successfully", body));
    }

    @Operation(
            summary = "Get all feedback for a candidate",
            description = "Returns all feedback entries for a candidate with per-category averages and overall average rating."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER', 'INTERVIEWER')")
    @GetMapping("/candidates/{candidateId}/feedback")
    public ResponseEntity<ApiResponse<CandidateFeedbackSummaryResponse>> getCandidateFeedback(
            @PathVariable String candidateId
    ) {
        CandidateFeedbackSummaryResponse summary = feedbackService.getCandidateFeedback(candidateId);
        return ResponseEntity.ok(ApiResponse.success("Candidate feedback retrieved successfully", summary));
    }
}
