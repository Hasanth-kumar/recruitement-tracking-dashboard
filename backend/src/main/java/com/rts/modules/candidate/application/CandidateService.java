package com.rts.modules.candidate.application;

import com.rts.modules.candidate.api.dto.CreateCandidateRequest;
import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.shared.exception.ConflictException;
import com.rts.shared.kernel.RecruitmentStage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;

    public CandidateService(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional
    public Candidate create(CreateCandidateRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (candidateRepository.existsByEmailIgnoreCaseAndDeletedFalse(normalizedEmail)) {
            throw new ConflictException("Candidate with this email already exists");
        }

        Candidate candidate = new Candidate();
        candidate.setName(request.name().trim());
        candidate.setEmail(normalizedEmail);
        candidate.setPhone(request.phone().trim());
        candidate.setPosition(request.position().trim());
        candidate.setStage(RecruitmentStage.APPLICATION_RECEIVED);

        return candidateRepository.save(candidate);
    }
}
