package com.rts.modules.candidate.persistence;

import com.rts.modules.candidate.domain.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, String> {

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    Optional<Candidate> findByIdAndDeletedFalse(String id);
}
