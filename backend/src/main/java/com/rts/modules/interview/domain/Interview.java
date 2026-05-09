package com.rts.modules.interview.domain;

import com.rts.shared.kernel.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interviews")
public class Interview extends BaseEntity {

    @Column(name = "candidate_id", nullable = false, length = 36)
    private String candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "round", nullable = false, length = 20)
    private InterviewRound round;

    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @ElementCollection
    @CollectionTable(name = "interview_interviewers", joinColumns = @JoinColumn(name = "interview_id"))
    @Column(name = "interviewer_username", nullable = false, length = 100)
    private List<String> interviewerUsernames = new ArrayList<>();

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public InterviewRound getRound() {
        return round;
    }

    public void setRound(InterviewRound round) {
        this.round = round;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }

    public List<String> getInterviewerUsernames() {
        return interviewerUsernames;
    }

    public void setInterviewerUsernames(List<String> interviewerUsernames) {
        this.interviewerUsernames = interviewerUsernames;
    }
}
