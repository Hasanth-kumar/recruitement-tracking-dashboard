package com.rts.modules.candidate.persistence;

import com.rts.modules.candidate.domain.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, String>, JpaSpecificationExecutor<Candidate> {

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    Optional<Candidate> findByIdAndDeletedFalse(String id);
}
