package com.rts.modules.feedback.domain;

import com.rts.shared.kernel.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
public class Feedback extends BaseEntity {

    @Column(name = "interview_id", nullable = false, length = 36)
    private String interviewId;

    @Column(name = "candidate_id", nullable = false, length = 36)
    private String candidateId;

    @Column(name = "submitted_by_username", nullable = false, length = 100)
    private String submittedByUsername;

    @Column(name = "technical_rating", nullable = false)
    private int technicalRating;

    @Column(name = "communication_rating", nullable = false)
    private int communicationRating;

    @Column(name = "problem_solving_rating", nullable = false)
    private int problemSolvingRating;

    @Column(name = "leadership_rating", nullable = false)
    private int leadershipRating;

    @Column(name = "culture_rating", nullable = false)
    private int cultureRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", nullable = false, length = 20)
    private FeedbackRecommendation recommendation;

    @Column(name = "comments", length = 1000)
    private String comments;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getSubmittedByUsername() {
        return submittedByUsername;
    }

    public void setSubmittedByUsername(String submittedByUsername) {
        this.submittedByUsername = submittedByUsername;
    }

    public int getTechnicalRating() {
        return technicalRating;
    }

    public void setTechnicalRating(int technicalRating) {
        this.technicalRating = technicalRating;
    }

    public int getCommunicationRating() {
        return communicationRating;
    }

    public void setCommunicationRating(int communicationRating) {
        this.communicationRating = communicationRating;
    }

    public int getProblemSolvingRating() {
        return problemSolvingRating;
    }

    public void setProblemSolvingRating(int problemSolvingRating) {
        this.problemSolvingRating = problemSolvingRating;
    }

    public int getLeadershipRating() {
        return leadershipRating;
    }

    public void setLeadershipRating(int leadershipRating) {
        this.leadershipRating = leadershipRating;
    }

    public int getCultureRating() {
        return cultureRating;
    }

    public void setCultureRating(int cultureRating) {
        this.cultureRating = cultureRating;
    }

    public FeedbackRecommendation getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(FeedbackRecommendation recommendation) {
        this.recommendation = recommendation;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
