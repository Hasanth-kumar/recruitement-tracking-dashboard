package com.rts.modules.interview.api.dto;

import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewRound;
import com.rts.modules.interview.domain.InterviewStatus;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewResponse(
        String id,
        String candidateId,
        String candidateName,
        InterviewRound round,
        LocalDateTime dateTime,
        Integer durationMinutes,
        String meetingLink,
        String location,
        String notes,
        InterviewStatus status,
        List<String> interviewerUsernames
) {
    public static InterviewResponse from(Interview interview) {
        return from(interview, null);
    }

    public static InterviewResponse from(Interview interview, String candidateName) {
        List<String> interviewers = interview.getInterviewerUsernames();
        // Materialize here: getInterviewerUsernames() may return a lazy PersistentBag; open-in-view is false.
        List<String> interviewerSnapshot = interviewers == null ? List.of() : List.copyOf(interviewers);
        return new InterviewResponse(
                interview.getId(),
                interview.getCandidateId(),
                candidateName,
                interview.getRound(),
                interview.getDateTime(),
                interview.getDurationMinutes(),
                interview.getMeetingLink(),
                interview.getLocation(),
                interview.getNotes(),
                interview.getStatus(),
                interviewerSnapshot
        );
    }
}
