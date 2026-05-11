package com.rts.modules.interview.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record RescheduleInterviewRequest(
        @NotNull(message = "dateTime is required")
        @Future(message = "dateTime must be in the future")
        LocalDateTime dateTime,

        @NotNull(message = "durationMinutes is required")
        Integer durationMinutes,

        @NotEmpty(message = "At least one interviewer is required")
        List<@NotBlank(message = "Interviewer username cannot be blank") String> interviewerUsernames,

        @Size(max = 500, message = "meetingLink must not exceed 500 characters")
        String meetingLink,

        @Size(max = 255, message = "location must not exceed 255 characters")
        String location,

        @Size(max = 1000, message = "notes must not exceed 1000 characters")
        String notes,

        @Size(max = 500, message = "rescheduleReason must not exceed 500 characters")
        String rescheduleReason
) {
}
