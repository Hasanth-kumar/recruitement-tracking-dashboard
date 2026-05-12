package com.rts.shared.events;

import com.rts.shared.kernel.RecruitmentStage;

public record CandidateStageChangedEvent(
        String candidateId,
        String candidateName,
        String candidateEmail,
        String position,
        RecruitmentStage previousStage,
        RecruitmentStage newStage,
        String changedBy
) {
}
