package com.rts.modules.candidate.persistence;

import com.rts.modules.candidate.domain.CandidateDocument;
import com.rts.modules.candidate.domain.CandidateDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateDocumentRepository extends JpaRepository<CandidateDocument, String> {

    Optional<CandidateDocument> findByCandidateIdAndDocumentTypeAndDeletedFalse(
            String candidateId,
            CandidateDocumentType documentType
    );
}
