package com.rts.modules.interview.api;

import com.rts.modules.interview.api.dto.InterviewResponse;
import com.rts.modules.interview.api.dto.InterviewPhotoUploadResponse;
import com.rts.modules.interview.api.dto.RescheduleInterviewRequest;
import com.rts.modules.interview.api.dto.CancelInterviewRequest;
import com.rts.modules.interview.api.dto.ScheduleRoundOneInterviewRequest;
import com.rts.modules.interview.api.dto.ScheduleRoundTwoInterviewRequest;
import com.rts.modules.interview.application.InterviewPhotoService;
import com.rts.modules.interview.application.InterviewService;
import com.rts.modules.interview.domain.InterviewPhoto;
import com.rts.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Interviews")
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;
    private final InterviewPhotoService interviewPhotoService;

    public InterviewController(InterviewService interviewService, InterviewPhotoService interviewPhotoService) {
        this.interviewService = interviewService;
        this.interviewPhotoService = interviewPhotoService;
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

    @Operation(
            summary = "Reschedule interview",
            description = "Reschedules an existing interview with conflict validation and notification update."
    )
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<ApiResponse<InterviewResponse>> rescheduleInterview(
            @PathVariable String id,
            @Valid @RequestBody RescheduleInterviewRequest request
    ) {
        InterviewResponse response = interviewService.rescheduleInterview(id, request);
        return ResponseEntity.ok(ApiResponse.success("Interview rescheduled successfully", response));
    }

    @Operation(
            summary = "Cancel interview",
            description = "Cancels a scheduled interview, marks status as CANCELLED, and rolls candidate stage back."
    )
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InterviewResponse>> cancelInterview(
            @PathVariable String id,
            @Valid @RequestBody(required = false) CancelInterviewRequest request
    ) {
        InterviewResponse response = interviewService.cancelInterview(id, request);
        return ResponseEntity.ok(ApiResponse.success("Interview cancelled successfully", response));
    }

    @Operation(
            summary = "Upload interview photos",
            description = "Uploads up to 10 JPG/PNG photos for an interview. Each file must be <= 5MB."
    )
    @PostMapping(path = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<InterviewPhotoUploadResponse>>> uploadInterviewPhotos(
            @PathVariable String id,
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart(value = "captions", required = false) List<String> captions
    ) {
        List<InterviewPhoto> photos = interviewPhotoService.uploadPhotos(id, files, captions);
        List<InterviewPhotoUploadResponse> response = photos.stream()
                .map(InterviewPhotoUploadResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Interview photos uploaded successfully", response));
    }
}
