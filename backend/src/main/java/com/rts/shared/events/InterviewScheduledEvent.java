package com.rts.shared.events;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewScheduledEvent(
        String interviewId,
        String candidateId,
        LocalDateTime scheduledAt,
        int durationMinutes,
        List<String> interviewerUsernames,
        String initiatedBy
) {
}
