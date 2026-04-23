package com.rts.modules.candidate.api.dto;

import com.rts.modules.candidate.domain.Candidate;
import com.rts.shared.kernel.RecruitmentStage;

import java.time.LocalDateTime;

public record CandidateResponse(
        String id,
        String name,
        String email,
        String phone,
        String position,
        RecruitmentStage stage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CandidateResponse from(Candidate candidate) {
        return new CandidateResponse(
                candidate.getId(),
                candidate.getName(),
                candidate.getEmail(),
                candidate.getPhone(),
                candidate.getPosition(),
                candidate.getStage(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt()
        );
    }
}
