package com.rts.modules.candidate.application;

import com.rts.modules.candidate.api.dto.CandidateResponse;
import com.rts.modules.candidate.api.dto.CreateCandidateRequest;
import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.modules.candidate.persistence.CandidateSpecifications;
import com.rts.shared.exception.ConflictException;
import com.rts.shared.kernel.RecruitmentStage;
import com.rts.shared.response.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    public CandidateService(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional(readOnly = true)
    public PagedResponse<CandidateResponse> list(
            RecruitmentStage stage,
            String position,
            Pageable pageable
    ) {
        Pageable sanitized = sanitizePageable(pageable);
        Page<Candidate> page = candidateRepository.findAll(
                CandidateSpecifications.build(stage, position),
                sanitized
        );
        List<CandidateResponse> content = page.getContent().stream()
                .map(CandidateResponse::from)
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

        return candidateRepository.save(candidate);
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
