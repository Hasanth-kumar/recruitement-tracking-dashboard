package com.rts.modules.interview.api.dto;

import jakarta.validation.constraints.Size;

public record CancelInterviewRequest(
        @Size(max = 500, message = "reason must not exceed 500 characters")
        String reason
) {
}
