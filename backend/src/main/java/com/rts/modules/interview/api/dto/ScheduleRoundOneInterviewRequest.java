package com.rts.modules.interview.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleRoundOneInterviewRequest(
        @NotBlank(message = "Candidate ID is required")
        String candidateId,
        @NotNull(message = "Interview date/time is required")
        @Future(message = "Interview date/time must be in the future")
        LocalDateTime dateTime,
        @NotNull(message = "Duration is required")
        Integer durationMinutes,
        @NotBlank(message = "Meeting link is required")
        @Pattern(regexp = "https?://.+", message = "Meeting link must be a valid URL")
        String meetingLink,
        @NotEmpty(message = "At least one interviewer is required")
        List<@NotBlank(message = "Interviewer username cannot be blank") String> interviewerUsernames,
        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {
}
