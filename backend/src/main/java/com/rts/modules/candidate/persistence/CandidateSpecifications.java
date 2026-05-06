package com.rts.modules.candidate.persistence;

import com.rts.modules.candidate.domain.Candidate;
import com.rts.shared.kernel.RecruitmentStage;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CandidateSpecifications {

    private CandidateSpecifications() {
    }

    public static Specification<Candidate> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Candidate> build(
            RecruitmentStage stage,
            String position,
            String search,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {
        Specification<Candidate> spec = Specification.where(notDeleted());
        if (stage != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("stage"), stage));
        }
        if (position != null && !position.isBlank()) {
            String pattern = "%" + position.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("position")), pattern));
        }
        if (search != null && !search.isBlank()) {
            String term = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), term),
                    cb.like(cb.lower(root.get("email")), term)
            ));
        }
        if (createdFrom != null) {
            LocalDateTime from = createdFrom.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (createdTo != null) {
            LocalDateTime toExclusive = createdTo.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), toExclusive));
        }
        return spec;
    }

}
