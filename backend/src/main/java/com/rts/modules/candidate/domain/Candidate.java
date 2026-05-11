package com.rts.modules.candidate.domain;

import com.rts.shared.kernel.BaseEntity;
import com.rts.shared.kernel.RecruitmentStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "candidates")
public class Candidate extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Column(name = "position", nullable = false, length = 100)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 50)
    private RecruitmentStage stage = RecruitmentStage.APPLICATION_RECEIVED;

    @Column(name = "experience", length = 200)
    private String experience;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "eval_score")
    private Double evalScore;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public RecruitmentStage getStage() {
        return stage;
    }

    public void setStage(RecruitmentStage stage) {
        this.stage = stage;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Double getEvalScore() {
        return evalScore;
    }

    public void setEvalScore(Double evalScore) {
        this.evalScore = evalScore;
    }
}
