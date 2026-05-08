package com.rts.modules.candidate.persistence;

import com.rts.modules.candidate.domain.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, String>, JpaSpecificationExecutor<Candidate> {

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByEmailIgnoreCaseAndDeletedFalseAndIdNot(String email, String id);

    Optional<Candidate> findByIdAndDeletedFalse(String id);

    List<Candidate> findByIdInAndDeletedFalse(Collection<String> ids);
}
