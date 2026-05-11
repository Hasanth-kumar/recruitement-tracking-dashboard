package com.rts.modules.candidate.application;

/**
 * Allows other modules to update the candidate aggregate evaluation score without reaching into repositories.
 */
public interface CandidateEvalPort {

    void applyEvalScore(String candidateId, Double evalScore);
}
