package com.rts.modules.candidate.api.dto;

import java.util.List;

public record BulkStageUpdateResponse(
        int requestedCount,
        int updatedCount,
        List<String> updatedCandidateIds
) {
}
