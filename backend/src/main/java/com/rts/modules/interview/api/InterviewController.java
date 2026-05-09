package com.rts.modules.interview.api;

import com.rts.modules.interview.api.dto.InterviewResponse;
import com.rts.modules.interview.api.dto.ScheduleRoundOneInterviewRequest;
import com.rts.modules.interview.api.dto.ScheduleRoundTwoInterviewRequest;
import com.rts.modules.interview.application.InterviewService;
import com.rts.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

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

    @Operation(summary = "Schedule Round 2 interview", description = "Schedules Round 2 interview with R1 cleared prerequisite and conflict validation.")
    @PostMapping("/round2")
    public ResponseEntity<ApiResponse<InterviewResponse>> scheduleRoundTwo(
            @Valid @RequestBody ScheduleRoundTwoInterviewRequest request
    ) {
        InterviewResponse response = interviewService.scheduleRoundTwo(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Round 2 interview scheduled successfully", response));
    }

    @Operation(
            summary = "Get interview schedule",
            description = "Returns scheduled interviews filtered by date-time range and optional interviewer username."
    )
    @GetMapping("/schedule")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getSchedule(
            @Parameter(description = "Inclusive start date-time (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDateTime,
            @Parameter(description = "Inclusive end date-time (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDateTime,
            @Parameter(description = "Optional interviewer username (case-insensitive)")
            @RequestParam(required = false) String interviewerUsername
    ) {
        List<InterviewResponse> schedule = interviewService.getSchedule(fromDateTime, toDateTime, interviewerUsername);
        return ResponseEntity.ok(ApiResponse.success("Interview schedule retrieved successfully", schedule));
    }
}
