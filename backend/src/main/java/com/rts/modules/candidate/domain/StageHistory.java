package com.rts.modules.candidate.domain;

import com.rts.shared.kernel.BaseEntity;
import com.rts.shared.kernel.RecruitmentStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "stage_history")
public class StageHistory extends BaseEntity {

    @Column(name = "candidate_id", nullable = false, length = 36)
    private String candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 80)
    private RecruitmentStage stage;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public RecruitmentStage getStage() {
        return stage;
    }

    public void setStage(RecruitmentStage stage) {
        this.stage = stage;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}
