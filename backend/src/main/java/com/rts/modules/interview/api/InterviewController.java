package com.rts.modules.interview.api;

import com.rts.modules.interview.api.dto.InterviewResponse;
import com.rts.modules.interview.api.dto.ScheduleRoundOneInterviewRequest;
import com.rts.modules.interview.application.InterviewService;
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

@Tag(name = "Interviews")
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @Operation(summary = "Schedule Round 1 interview", description = "Schedules Round 1 interview with conflict validation and stage auto-update.")
    @PostMapping("/round1")
    public ResponseEntity<ApiResponse<InterviewResponse>> scheduleRoundOne(
            @Valid @RequestBody ScheduleRoundOneInterviewRequest request
    ) {
        InterviewResponse response = interviewService.scheduleRoundOne(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Round 1 interview scheduled successfully", response));
    }
}
