package com.rts.shared.events;

public record CandidateRegisteredEvent(
        String candidateId,
        String candidateName,
        String candidateEmail,
        String position
) {
}
