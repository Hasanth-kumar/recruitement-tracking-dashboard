package com.rts.modules.candidate.api.dto;

import com.rts.modules.candidate.domain.CandidateDocument;
import com.rts.modules.candidate.domain.CandidateDocumentType;

import java.time.LocalDateTime;

public record DocumentUploadResponse(
        String documentId,
        String candidateId,
        CandidateDocumentType documentType,
        String originalFileName,
        String filePath,
        long fileSize,
        LocalDateTime linkedAt
) {
    public static DocumentUploadResponse from(CandidateDocument document) {
        return new DocumentUploadResponse(
                document.getId(),
                document.getCandidate().getId(),
                document.getDocumentType(),
                document.getOriginalFileName(),
                document.getFilePath(),
                document.getFileSize(),
                document.getLinkedAt()
        );
    }
}
