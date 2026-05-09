package com.rts.modules.interview.application;

import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.domain.StageHistory;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.modules.candidate.persistence.StageHistoryRepository;
import com.rts.modules.interview.api.dto.InterviewResponse;
import com.rts.modules.interview.api.dto.ScheduleRoundOneInterviewRequest;
import com.rts.modules.interview.api.dto.ScheduleRoundTwoInterviewRequest;
import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewRound;
import com.rts.modules.interview.domain.InterviewStatus;
import com.rts.modules.interview.persistence.InterviewRepository;
import com.rts.shared.events.InterviewScheduledEvent;
import com.rts.shared.exception.ConflictException;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
import com.rts.shared.kernel.RecruitmentStage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class InterviewService {

    private static final Set<Integer> ALLOWED_DURATIONS = Set.of(30, 45, 60);

    private final InterviewRepository interviewRepository;
    private final CandidateRepository candidateRepository;
    private final StageHistoryRepository stageHistoryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public InterviewService(
            InterviewRepository interviewRepository,
            CandidateRepository candidateRepository,
            StageHistoryRepository stageHistoryRepository,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.interviewRepository = interviewRepository;
        this.candidateRepository = candidateRepository;
        this.stageHistoryRepository = stageHistoryRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional
    public InterviewResponse scheduleRoundOne(ScheduleRoundOneInterviewRequest request) {
        validateDuration(request.durationMinutes());
        List<String> normalizedInterviewers = normalizeInterviewers(request.interviewerUsernames());

        Candidate candidate = candidateRepository.findByIdAndDeletedFalse(request.candidateId().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + request.candidateId()));

        LocalDateTime requestedStart = request.dateTime();
        LocalDateTime requestedEnd = requestedStart.plusMinutes(request.durationMinutes());
        ensureNoConflict(normalizedInterviewers, requestedStart, requestedEnd);

        Interview interview = new Interview();
        interview.setCandidateId(candidate.getId());
        interview.setRound(InterviewRound.ROUND_1);
        interview.setDateTime(requestedStart);
        interview.setDurationMinutes(request.durationMinutes());
        interview.setMeetingLink(request.meetingLink().trim());
        interview.setLocation(null);
        interview.setNotes(trimToNull(request.notes()));
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setInterviewerUsernames(normalizedInterviewers);
        Interview savedInterview = interviewRepository.save(interview);

        String changedBy = resolveAuthenticatedUser();
        candidate.setStage(RecruitmentStage.R1_SCHEDULED);
        candidateRepository.save(candidate);

        StageHistory stageHistory = new StageHistory();
        stageHistory.setCandidateId(candidate.getId());
        stageHistory.setStage(RecruitmentStage.R1_SCHEDULED);
        stageHistory.setChangedAt(LocalDateTime.now());
        stageHistory.setChangedBy(changedBy);
        stageHistoryRepository.save(stageHistory);

        applicationEventPublisher.publishEvent(new InterviewScheduledEvent(
                savedInterview.getId(),
                savedInterview.getCandidateId(),
                savedInterview.getRound(),
                savedInterview.getDateTime(),
                savedInterview.getDurationMinutes(),
                savedInterview.getInterviewerUsernames(),
                changedBy
        ));

        return InterviewResponse.from(savedInterview);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional
    public InterviewResponse scheduleRoundTwo(ScheduleRoundTwoInterviewRequest request) {
        validateDuration(request.durationMinutes());
        List<String> normalizedInterviewers = normalizeInterviewers(request.interviewerUsernames());

        Candidate candidate = candidateRepository.findByIdAndDeletedFalse(request.candidateId().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + request.candidateId()));
        if (candidate.getStage() != RecruitmentStage.R1_CLEARED) {
            throw new ValidationException("Round 2 can only be scheduled for candidates in stage: R1_CLEARED");
        }

        String location = normalizeLocation(request.location());
        LocalDateTime requestedStart = request.dateTime();
        LocalDateTime requestedEnd = requestedStart.plusMinutes(request.durationMinutes());
        ensureNoConflict(normalizedInterviewers, requestedStart, requestedEnd);

        Interview interview = new Interview();
        interview.setCandidateId(candidate.getId());
        interview.setRound(InterviewRound.ROUND_2);
        interview.setDateTime(requestedStart);
        interview.setDurationMinutes(request.durationMinutes());
        interview.setMeetingLink(null);
        interview.setLocation(location);
        interview.setNotes(trimToNull(request.notes()));
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setInterviewerUsernames(normalizedInterviewers);
        Interview savedInterview = interviewRepository.save(interview);

        String changedBy = resolveAuthenticatedUser();
        candidate.setStage(RecruitmentStage.R2_SCHEDULED);
        candidateRepository.save(candidate);

        StageHistory stageHistory = new StageHistory();
        stageHistory.setCandidateId(candidate.getId());
        stageHistory.setStage(RecruitmentStage.R2_SCHEDULED);
        stageHistory.setChangedAt(LocalDateTime.now());
        stageHistory.setChangedBy(changedBy);
        stageHistoryRepository.save(stageHistory);

        applicationEventPublisher.publishEvent(new InterviewScheduledEvent(
                savedInterview.getId(),
                savedInterview.getCandidateId(),
                savedInterview.getRound(),
                savedInterview.getDateTime(),
                savedInterview.getDurationMinutes(),
                savedInterview.getInterviewerUsernames(),
                changedBy
        ));

        return InterviewResponse.from(savedInterview);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER', 'INTERVIEWER')")
    @Transactional(readOnly = true)
    public List<InterviewResponse> getSchedule(LocalDateTime fromDateTime, LocalDateTime toDateTime, String interviewerUsername) {
        if (fromDateTime == null || toDateTime == null) {
            throw new ValidationException("fromDateTime and toDateTime are required");
        }
        if (toDateTime.isBefore(fromDateTime)) {
            throw new ValidationException("toDateTime must be on or after fromDateTime");
        }
        String normalizedInterviewer = trimToNull(interviewerUsername);
        if (normalizedInterviewer != null) {
            normalizedInterviewer = normalizedInterviewer.toLowerCase(Locale.ROOT);
        }

        return interviewRepository.findSchedule(
                        InterviewStatus.SCHEDULED,
                        fromDateTime,
                        toDateTime,
                        normalizedInterviewer
                ).stream()
                .map(InterviewResponse::from)
                .toList();
    }

    private void validateDuration(Integer durationMinutes) {
        if (durationMinutes == null || !ALLOWED_DURATIONS.contains(durationMinutes)) {
            throw new ValidationException("Duration must be one of: 30, 45, 60 minutes");
        }
    }

    private List<String> normalizeInterviewers(List<String> interviewerUsernames) {
        if (interviewerUsernames == null || interviewerUsernames.isEmpty()) {
            throw new ValidationException("At least one interviewer is required");
        }

        List<String> normalized = interviewerUsernames.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new ValidationException("At least one valid interviewer is required");
        }
        return normalized;
    }

    private void ensureNoConflict(List<String> interviewerUsernames, LocalDateTime requestedStart, LocalDateTime requestedEnd) {
        LocalDateTime windowStart = requestedStart.minusHours(4);
        LocalDateTime windowEnd = requestedEnd.plusHours(4);
        List<Interview> nearbyScheduledInterviews = interviewRepository.findByStatusInAndDateTimeBetween(
                List.of(InterviewStatus.SCHEDULED), windowStart, windowEnd
        );

        Set<String> requested = new HashSet<>(interviewerUsernames);
        for (Interview existing : nearbyScheduledInterviews) {
            LocalDateTime existingStart = existing.getDateTime();
            LocalDateTime existingEnd = existingStart.plusMinutes(existing.getDurationMinutes());
            boolean overlaps = requestedStart.isBefore(existingEnd) && requestedEnd.isAfter(existingStart);
            if (!overlaps) {
                continue;
            }

            boolean interviewerConflict = existing.getInterviewerUsernames().stream()
                    .map(String::toLowerCase)
                    .anyMatch(requested::contains);
            if (interviewerConflict) {
                throw new ConflictException("Scheduling conflict detected for one or more interviewers");
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new ValidationException("Location is required");
        }
        String trimmed = location.trim();
        if (trimmed.length() > 255) {
            throw new ValidationException("Location must not exceed 255 characters");
        }
        return trimmed;
    }

    private String resolveAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "system";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String username && !username.isBlank()) {
            return username;
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? "system" : name;
    }
}
