package com.rts.modules.interview.api.dto;

import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewRound;
import com.rts.modules.interview.domain.InterviewStatus;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewResponse(
        String id,
        String candidateId,
        InterviewRound round,
        LocalDateTime dateTime,
        Integer durationMinutes,
        String meetingLink,
        String notes,
        InterviewStatus status,
        List<String> interviewerUsernames
) {
    public static InterviewResponse from(Interview interview) {
        return new InterviewResponse(
                interview.getId(),
                interview.getCandidateId(),
                interview.getRound(),
                interview.getDateTime(),
                interview.getDurationMinutes(),
                interview.getMeetingLink(),
                interview.getNotes(),
                interview.getStatus(),
                interview.getInterviewerUsernames()
        );
    }
}
