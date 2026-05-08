package com.rts.modules.candidate.api.dto;

import com.rts.shared.kernel.RecruitmentStage;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkStageUpdateRequest(
        @NotEmpty(message = "Candidate IDs are required")
        List<String> candidateIds,
        @NotNull(message = "Target stage is required")
        RecruitmentStage stage
) {
}
