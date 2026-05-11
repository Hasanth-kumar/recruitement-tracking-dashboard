package com.rts.modules.candidate.application;

import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateEvalAdapter implements CandidateEvalPort {

    private final CandidateRepository candidateRepository;

    public CandidateEvalAdapter(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @Override
    @Transactional
    public void applyEvalScore(String candidateId, Double evalScore) {
        Candidate candidate = candidateRepository.findByIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));
        candidate.setEvalScore(evalScore);
        candidateRepository.save(candidate);
    }
}
