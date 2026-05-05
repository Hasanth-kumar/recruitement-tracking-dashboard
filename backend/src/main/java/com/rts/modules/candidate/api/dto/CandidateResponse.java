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
        String experience,
        String notes,
        boolean hasPhoto,
        boolean hasResume,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CandidateResponse from(Candidate candidate) {
        return from(candidate, false, false);
    }

    public static CandidateResponse from(Candidate candidate, boolean hasPhoto, boolean hasResume) {
        return new CandidateResponse(
                candidate.getId(),
                candidate.getName(),
                candidate.getEmail(),
                candidate.getPhone(),
                candidate.getPosition(),
                candidate.getStage(),
                blankToEmpty(candidate.getExperience()),
                candidate.getNotes(),
                hasPhoto,
                hasResume,
                candidate.getCreatedAt(),
                candidate.getUpdatedAt()
        );
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value;
    }
}
