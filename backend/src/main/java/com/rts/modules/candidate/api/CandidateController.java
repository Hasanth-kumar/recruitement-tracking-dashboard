package com.rts.modules.candidate.api;

import com.rts.modules.candidate.api.dto.CandidateResponse;
import com.rts.modules.candidate.api.dto.CreateCandidateRequest;
import com.rts.modules.candidate.application.CandidateService;
import com.rts.modules.candidate.domain.Candidate;
import com.rts.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Candidates")
@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @Operation(summary = "Create candidate", description = "Creates a candidate with validation and UUID identity.")
    @PostMapping
    public ResponseEntity<ApiResponse<CandidateResponse>> create(@Valid @RequestBody CreateCandidateRequest request) {
        Candidate created = candidateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Candidate created successfully", CandidateResponse.from(created)));
    }
}
