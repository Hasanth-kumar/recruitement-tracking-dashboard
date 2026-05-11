package com.rts.modules.feedback.api;

import com.rts.modules.feedback.api.dto.FeedbackResponse;
import com.rts.modules.feedback.api.dto.SubmitFeedbackRequest;
import com.rts.modules.feedback.application.FeedbackService;
import com.rts.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Feedback")
@RestController
@RequestMapping("/api/feedback")
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
    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> submit(@Valid @RequestBody SubmitFeedbackRequest request) {
        FeedbackResponse body = feedbackService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Feedback submitted successfully", body));
    }
}
