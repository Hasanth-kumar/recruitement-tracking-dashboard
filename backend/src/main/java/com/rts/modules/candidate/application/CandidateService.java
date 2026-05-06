package com.rts.modules.candidate.application;

import com.rts.modules.candidate.api.dto.CandidateResponse;
import com.rts.modules.candidate.api.dto.CreateCandidateRequest;
import com.rts.modules.candidate.api.dto.StageHistoryResponse;
import com.rts.modules.candidate.api.dto.UpdateCandidateRequest;
import com.rts.modules.candidate.api.dto.UpdateStageRequest;
import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.domain.CandidateDocumentType;
import com.rts.modules.candidate.domain.StageHistory;
import com.rts.modules.candidate.persistence.CandidateDocumentRepository;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.modules.candidate.persistence.CandidateSpecifications;
import com.rts.modules.candidate.persistence.StageHistoryRepository;
import com.rts.shared.exception.ConflictException;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
import com.rts.shared.kernel.RecruitmentStage;
import com.rts.shared.response.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CandidateService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "name", "email", "position", "stage", "createdAt", "updatedAt"
    );

    private final CandidateRepository candidateRepository;
    private final CandidateDocumentRepository candidateDocumentRepository;
    private final StageHistoryRepository stageHistoryRepository;

    public CandidateService(
            CandidateRepository candidateRepository,
            CandidateDocumentRepository candidateDocumentRepository,
            StageHistoryRepository stageHistoryRepository
    ) {
        this.candidateRepository = candidateRepository;
        this.candidateDocumentRepository = candidateDocumentRepository;
        this.stageHistoryRepository = stageHistoryRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional(readOnly = true)
    public PagedResponse<CandidateResponse> list(
            RecruitmentStage stage,
            String position,
            String search,
            LocalDate createdFrom,
            LocalDate createdTo,
            Pageable pageable
    ) {
        Pageable sanitized = sanitizePageable(pageable);
        Specification<Candidate> specification =
                CandidateSpecifications.build(stage, position, search, createdFrom, createdTo);
        Page<Candidate> page = candidateRepository.findAll(specification, sanitized);
        List<String> ids = page.getContent().stream().map(Candidate::getId).toList();
        Set<String> photoIds = ids.isEmpty()
                ? Set.of()
                : new HashSet<>(candidateDocumentRepository.findCandidateIdsWithDocumentType(
                CandidateDocumentType.PHOTO, ids));
        Set<String> resumeIds = ids.isEmpty()
                ? Set.of()
                : new HashSet<>(candidateDocumentRepository.findCandidateIdsWithDocumentType(
                CandidateDocumentType.RESUME, ids));

        List<CandidateResponse> content = page.getContent().stream()
                .map(c -> CandidateResponse.from(c, photoIds.contains(c.getId()), resumeIds.contains(c.getId())))
                .toList();
        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
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
        candidate.setExperience(trimToNull(request.experience()));
        candidate.setNotes(trimToNull(request.notes()));

        return candidateRepository.save(candidate);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional(readOnly = true)
    public CandidateResponse getById(String id) {
        Candidate candidate = candidateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + id));
        boolean hasPhoto = candidateDocumentRepository
                .findByCandidateIdAndDocumentTypeAndDeletedFalse(id, CandidateDocumentType.PHOTO)
                .isPresent();
        boolean hasResume = candidateDocumentRepository
                .findByCandidateIdAndDocumentTypeAndDeletedFalse(id, CandidateDocumentType.RESUME)
                .isPresent();
        return CandidateResponse.from(candidate, hasPhoto, hasResume);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional
    public CandidateResponse update(String id, UpdateCandidateRequest request) {
        Candidate candidate = candidateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + id));

        String normalizedEmail = request.email().trim().toLowerCase();
        if (candidateRepository.existsByEmailIgnoreCaseAndDeletedFalseAndIdNot(normalizedEmail, id)) {
            throw new ConflictException("Candidate with this email already exists");
        }

        candidate.setName(request.name().trim());
        candidate.setEmail(normalizedEmail);
        candidate.setPhone(request.phone().trim());
        candidate.setPosition(request.position().trim());
        candidate.setExperience(trimToNull(request.experience()));
        candidate.setNotes(trimToNull(request.notes()));

        return toResponseWithDocumentFlags(candidateRepository.save(candidate));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional
    public void softDelete(String id) {
        Candidate candidate = candidateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + id));
        candidate.setDeleted(true);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional
    public CandidateResponse updateStage(String id, UpdateStageRequest request) {
        Candidate candidate = candidateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + id));
        if (candidate.getStage() == request.stage()) {
            throw new ValidationException("Candidate is already in stage: " + request.stage().name());
        }
        candidate.setStage(request.stage());
        Candidate savedCandidate = candidateRepository.save(candidate);

        StageHistory stageHistory = new StageHistory();
        stageHistory.setCandidateId(savedCandidate.getId());
        stageHistory.setStage(request.stage());
        stageHistory.setChangedAt(LocalDateTime.now());
        stageHistory.setChangedBy(resolveAuthenticatedUser());
        stageHistoryRepository.save(stageHistory);

        return toResponseWithDocumentFlags(savedCandidate);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional(readOnly = true)
    public List<StageHistoryResponse> getStageHistory(String id) {
        candidateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + id));
        return stageHistoryRepository.findByCandidateIdOrderByChangedAtDesc(id)
                .stream()
                .map(StageHistoryResponse::from)
                .toList();
    }

    private CandidateResponse toResponseWithDocumentFlags(Candidate candidate) {
        String id = candidate.getId();
        boolean hasPhoto = candidateDocumentRepository
                .findByCandidateIdAndDocumentTypeAndDeletedFalse(id, CandidateDocumentType.PHOTO)
                .isPresent();
        boolean hasResume = candidateDocumentRepository
                .findByCandidateIdAndDocumentTypeAndDeletedFalse(id, CandidateDocumentType.RESUME)
                .isPresent();
        return CandidateResponse.from(candidate, hasPhoto, hasResume);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "system";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String username && !username.isBlank()) {
            return username;
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? "system" : name;
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = pageable.getPageSize();
        if (size < 1) {
            size = DEFAULT_PAGE_SIZE;
        } else if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }

        Sort sort = pageable.getSort();
        if (sort == null || sort.isUnsorted()) {
            return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        }

        List<Sort.Order> allowedOrders = new ArrayList<>();
        for (Sort.Order order : sort) {
            if (ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                allowedOrders.add(order);
            }
        }
        if (allowedOrders.isEmpty()) {
            return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        }
        return PageRequest.of(page, size, Sort.by(allowedOrders));
    }
}
