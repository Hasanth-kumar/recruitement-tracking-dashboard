package com.rts.shared.events;

import com.rts.modules.interview.domain.InterviewRound;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewScheduledEvent(
        String interviewId,
        String candidateId,
        InterviewRound round,
        LocalDateTime scheduledAt,
        int durationMinutes,
        List<String> interviewerUsernames,
        String initiatedBy
) {
}
