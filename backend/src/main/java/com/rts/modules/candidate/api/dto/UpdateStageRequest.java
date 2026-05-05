package com.rts.modules.candidate.api.dto;

import com.rts.shared.kernel.RecruitmentStage;
import jakarta.validation.constraints.NotNull;

public record UpdateStageRequest(
        @NotNull(message = "Stage is required")
        RecruitmentStage stage
) {
}
