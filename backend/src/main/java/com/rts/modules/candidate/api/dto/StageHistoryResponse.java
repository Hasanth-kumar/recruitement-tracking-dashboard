package com.rts.modules.candidate.api.dto;

import com.rts.modules.candidate.domain.StageHistory;
import com.rts.shared.kernel.RecruitmentStage;

import java.time.LocalDateTime;

public record StageHistoryResponse(
        RecruitmentStage stage,
        LocalDateTime changedAt,
        String changedBy
) {
    public static StageHistoryResponse from(StageHistory stageHistory) {
        return new StageHistoryResponse(
                stageHistory.getStage(),
                stageHistory.getChangedAt(),
                stageHistory.getChangedBy()
        );
    }
}
