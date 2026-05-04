package com.rts.modules.candidate.persistence;

import com.rts.modules.candidate.domain.Candidate;
import com.rts.shared.kernel.RecruitmentStage;
import org.springframework.data.jpa.domain.Specification;

public final class CandidateSpecifications {

    private CandidateSpecifications() {
    }

    public static Specification<Candidate> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Candidate> build(RecruitmentStage stage, String position) {
        Specification<Candidate> spec = Specification.where(notDeleted());
        if (stage != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("stage"), stage));
        }
        if (position != null && !position.isBlank()) {
            String pattern = "%" + position.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("position")), pattern));
        }
        return spec;
    }
}
