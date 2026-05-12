package com.rts.shared.events;

import com.rts.modules.interview.domain.InterviewRound;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewRescheduledEvent(
        String interviewId,
        String candidateId,
        String candidateName,
        String candidateEmail,
        InterviewRound round,
        LocalDateTime previousDateTime,
        LocalDateTime newDateTime,
        int durationMinutes,
        List<String> interviewerUsernames,
        String meetingLink,
        String location,
        String notes,
        String initiatedBy,
        String reason
) {
}
