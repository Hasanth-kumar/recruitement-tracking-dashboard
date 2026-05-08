package com.rts.modules.candidate.api;

import com.rts.modules.candidate.api.dto.CandidateResponse;
import com.rts.modules.candidate.api.dto.CreateCandidateRequest;
import com.rts.modules.candidate.api.dto.BulkStageUpdateRequest;
import com.rts.modules.candidate.api.dto.BulkStageUpdateResponse;
import com.rts.modules.candidate.api.dto.StageHistoryResponse;
import com.rts.modules.candidate.api.dto.UpdateCandidateRequest;
import com.rts.modules.candidate.api.dto.UpdateStageRequest;
import com.rts.modules.candidate.application.CandidateService;
import com.rts.modules.candidate.domain.Candidate;
import com.rts.shared.kernel.RecruitmentStage;
import com.rts.shared.response.ApiResponse;
import com.rts.shared.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Candidates")
@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @Operation(
            summary = "List candidates (paginated)",
            description = "Returns a page of non-deleted candidates. Optional filters: stage, position (contains, case-insensitive), "
                    + "search (contains match on name or email), createdFrom / createdTo (filters `createdAt` date range, inclusive). "
                    + "Sort via repeated `sort` params (e.g. sort=name,asc&sort=createdAt,desc). Allowed sort fields: "
                    + "name, email, position, stage, createdAt, updatedAt. Default page size is 20."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CandidateResponse>>> list(
            @Parameter(description = "Filter by recruitment stage")
            @RequestParam(required = false) RecruitmentStage stage,
            @Parameter(description = "Case-insensitive contains match on position title")
            @RequestParam(required = false) String position,
            @Parameter(description = "Case-insensitive contains match on candidate name or email")
            @RequestParam(required = false) String search,
            @Parameter(description = "Include candidates created on or after this date (UTC day boundary)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @Parameter(description = "Include candidates created on or before this date (UTC day boundary, inclusive)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<CandidateResponse> page = candidateService.list(stage, position, search, createdFrom, createdTo, pageable);
        return ResponseEntity.ok(ApiResponse.success("Candidates retrieved successfully", page));
    }

    @Operation(summary = "Get candidate by id", description = "Returns one non-deleted candidate with document flags.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateResponse>> getById(@PathVariable String id) {
        CandidateResponse dto = candidateService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("Candidate retrieved successfully", dto));
    }

    @Operation(summary = "Create candidate", description = "Creates a candidate with validation and UUID identity.")
    @PostMapping
    public ResponseEntity<ApiResponse<CandidateResponse>> create(@Valid @RequestBody CreateCandidateRequest request) {
        Candidate created = candidateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Candidate created successfully", CandidateResponse.from(created)));
    }

    @Operation(summary = "Update candidate", description = "Updates core candidate fields.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateCandidateRequest request
    ) {
        CandidateResponse updated = candidateService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Candidate updated successfully", updated));
    }

    @Operation(summary = "Soft-delete candidate", description = "Marks the candidate as deleted.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable String id) {
        candidateService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.success("Candidate deleted successfully", null));
    }

    @Operation(summary = "Update recruitment stage", description = "Sets the candidate's current stage.")
    @PutMapping("/{id}/stage")
    public ResponseEntity<ApiResponse<CandidateResponse>> updateStage(
            @PathVariable String id,
            @Valid @RequestBody UpdateStageRequest request
    ) {
        CandidateResponse updated = candidateService.updateStage(id, request);
        return ResponseEntity.ok(ApiResponse.success("Stage updated successfully", updated));
    }

    @Operation(summary = "Get stage history", description = "Returns stage change history for the candidate.")
    @GetMapping("/{id}/stage-history")
    public ResponseEntity<ApiResponse<List<StageHistoryResponse>>> getStageHistory(@PathVariable String id) {
        List<StageHistoryResponse> history = candidateService.getStageHistory(id);
        return ResponseEntity.ok(ApiResponse.success("Stage history retrieved successfully", history));
    }

    @Operation(summary = "Bulk update candidate stage", description = "Updates stage for multiple candidates and logs individual history records.")
    @PostMapping("/bulk-stage")
    public ResponseEntity<ApiResponse<BulkStageUpdateResponse>> bulkStageUpdate(
            @Valid @RequestBody BulkStageUpdateRequest request
    ) {
        BulkStageUpdateResponse response = candidateService.bulkUpdateStage(request.candidateIds(), request.stage());
        return ResponseEntity.ok(ApiResponse.success("Bulk stage update completed successfully", response));
    }
}
