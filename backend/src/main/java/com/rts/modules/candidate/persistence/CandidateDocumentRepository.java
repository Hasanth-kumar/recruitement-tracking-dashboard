package com.rts.modules.candidate.persistence;

import com.rts.modules.candidate.domain.CandidateDocument;
import com.rts.modules.candidate.domain.CandidateDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CandidateDocumentRepository extends JpaRepository<CandidateDocument, String> {

    Optional<CandidateDocument> findByCandidateIdAndDocumentTypeAndDeletedFalse(
            String candidateId,
            CandidateDocumentType documentType
    );

    @Query("""
            select c.id from CandidateDocument d join d.candidate c
            where d.deleted = false and d.documentType = :type and c.id in :ids
            """)
    List<String> findCandidateIdsWithDocumentType(
            @Param("type") CandidateDocumentType type,
            @Param("ids") Collection<String> ids
    );
}
