package com.rts.modules.candidate.api;

import com.rts.modules.candidate.api.dto.DocumentUploadResponse;
import com.rts.modules.candidate.application.DocumentService;
import com.rts.modules.candidate.domain.CandidateDocument;
import com.rts.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Candidate Documents")
@RestController
@RequestMapping("/api/candidates")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(
            summary = "Upload candidate resume",
            description = "Uploads a candidate resume (PDF/DOC/DOCX, max 5MB). Replaces existing resume if present."
    )
    @PostMapping(path = "/{id}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadResume(
            @PathVariable("id") String candidateId,
            @RequestPart("file") MultipartFile file
    ) {
        CandidateDocument document = documentService.uploadResume(candidateId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resume uploaded successfully", DocumentUploadResponse.from(document)));
    }

    @Operation(
            summary = "Upload candidate photo",
            description = "Uploads a candidate photo (JPG/PNG, max 2MB) with server-side compression. Replaces existing photo if present."
    )
    @PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadPhoto(
            @PathVariable("id") String candidateId,
            @RequestPart("file") MultipartFile file
    ) {
        CandidateDocument document = documentService.uploadPhoto(candidateId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Photo uploaded successfully", DocumentUploadResponse.from(document)));
    }

    @Operation(summary = "Download candidate photo", description = "Returns the stored photo bytes (JPG/PNG).")
    @GetMapping(path = "/{id}/photo")
    public ResponseEntity<Resource> downloadPhoto(@PathVariable("id") String candidateId) {
        DocumentService.ServedDocument served = documentService.loadPhoto(candidateId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(served.contentType()))
                .body(served.resource());
    }

    @Operation(summary = "Download candidate resume", description = "Returns the stored resume file.")
    @GetMapping(path = "/{id}/resume")
    public ResponseEntity<Resource> downloadResume(@PathVariable("id") String candidateId) {
        DocumentService.ServedDocument served = documentService.loadResume(candidateId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(served.originalFileName(), java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(served.contentType()))
                .body(served.resource());
    }
}
