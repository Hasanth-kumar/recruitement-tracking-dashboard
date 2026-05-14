package com.rts.modules.interview.application;

import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.domain.StageHistory;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.modules.candidate.persistence.StageHistoryRepository;
import com.rts.modules.interview.api.dto.InterviewResponse;
import com.rts.modules.interview.api.dto.RescheduleInterviewRequest;
import com.rts.modules.interview.api.dto.CancelInterviewRequest;
import com.rts.modules.interview.api.dto.ScheduleRoundOneInterviewRequest;
import com.rts.modules.interview.api.dto.ScheduleRoundTwoInterviewRequest;
import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewActionType;
import com.rts.modules.interview.domain.InterviewHistory;
import com.rts.modules.interview.domain.InterviewRound;
import com.rts.modules.interview.domain.InterviewStatus;
import com.rts.modules.interview.persistence.InterviewHistoryRepository;
import com.rts.modules.interview.persistence.InterviewRepository;
import com.rts.shared.events.InterviewCancelledEvent;
import com.rts.shared.events.InterviewRescheduledEvent;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InterviewService {

    private static final Set<Integer> ALLOWED_DURATIONS = Set.of(30, 45, 60);
    private static final int MAX_NOTES_LENGTH = 1000;

    private final InterviewRepository interviewRepository;
    private final CandidateRepository candidateRepository;
    private final StageHistoryRepository stageHistoryRepository;
    private final InterviewHistoryRepository interviewHistoryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public InterviewService(
            InterviewRepository interviewRepository,
            CandidateRepository candidateRepository,
            StageHistoryRepository stageHistoryRepository,
            InterviewHistoryRepository interviewHistoryRepository,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.interviewRepository = interviewRepository;
        this.candidateRepository = candidateRepository;
        this.stageHistoryRepository = stageHistoryRepository;
        this.interviewHistoryRepository = interviewHistoryRepository;
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
                candidate.getName(),
                candidate.getEmail(),
                savedInterview.getRound(),
                savedInterview.getDateTime(),
                savedInterview.getDurationMinutes(),
                snapshotInterviewerUsernames(savedInterview),
                savedInterview.getMeetingLink(),
                savedInterview.getLocation(),
                savedInterview.getNotes(),
                changedBy
        ));

        return InterviewResponse.from(savedInterview, candidate.getName());
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
                candidate.getName(),
                candidate.getEmail(),
                savedInterview.getRound(),
                savedInterview.getDateTime(),
                savedInterview.getDurationMinutes(),
                snapshotInterviewerUsernames(savedInterview),
                savedInterview.getMeetingLink(),
                savedInterview.getLocation(),
                savedInterview.getNotes(),
                changedBy
        ));

        return InterviewResponse.from(savedInterview, candidate.getName());
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

        List<Interview> interviews = interviewRepository.findSchedule(
                InterviewStatus.SCHEDULED,
                fromDateTime,
                toDateTime,
                normalizedInterviewer
        );
        if (interviews.isEmpty()) {
            return List.of();
        }
        List<String> candidateIds = interviews.stream()
                .map(Interview::getCandidateId)
                .distinct()
                .toList();
        Map<String, String> namesByCandidateId = candidateRepository.findByIdInAndDeletedFalse(candidateIds).stream()
                .collect(Collectors.toMap(Candidate::getId, Candidate::getName));
        return interviews.stream()
                .map(i -> InterviewResponse.from(i, namesByCandidateId.get(i.getCandidateId())))
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional
    public InterviewResponse rescheduleInterview(String interviewId, RescheduleInterviewRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));

        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new ValidationException("Only scheduled interviews can be rescheduled");
        }

        validateDuration(request.durationMinutes());
        List<String> normalizedInterviewers = normalizeInterviewers(request.interviewerUsernames());
        LocalDateTime requestedStart = request.dateTime();
        LocalDateTime requestedEnd = requestedStart.plusMinutes(request.durationMinutes());
        ensureNoConflict(normalizedInterviewers, requestedStart, requestedEnd, interviewId);

        if (interview.getRound() == InterviewRound.ROUND_1) {
            String meetingLink = trimToNull(request.meetingLink());
            if (meetingLink == null) {
                throw new ValidationException("Meeting link is required for round 1 interviews");
            }
            interview.setMeetingLink(meetingLink);
            interview.setLocation(null);
        } else {
            interview.setMeetingLink(null);
            interview.setLocation(normalizeLocation(request.location()));
        }

        LocalDateTime previousDateTime = interview.getDateTime();
        Integer previousDuration = interview.getDurationMinutes();
        List<String> previousInterviewers = snapshotInterviewerUsernames(interview);
        String previousNotes = interview.getNotes();

        interview.setDateTime(requestedStart);
        interview.setDurationMinutes(request.durationMinutes());
        interview.setInterviewerUsernames(normalizedInterviewers);

        String changedBy = resolveAuthenticatedUser();
        String historyLine = buildRescheduleHistory(
                changedBy,
                previousDateTime,
                previousDuration,
                previousInterviewers,
                requestedStart,
                request.durationMinutes(),
                normalizedInterviewers,
                request.rescheduleReason()
        );
        interview.setNotes(mergeNotesWithHistory(request.notes(), previousNotes, historyLine));
        saveInterviewHistory(savedHistory(
                interview.getId(),
                InterviewActionType.RESCHEDULED,
                historyLine,
                changedBy
        ));

        Interview savedInterview = interviewRepository.save(interview);

        Candidate candidate = candidateRepository.findByIdAndDeletedFalse(savedInterview.getCandidateId())
                .orElse(null);

        applicationEventPublisher.publishEvent(new InterviewRescheduledEvent(
                savedInterview.getId(),
                savedInterview.getCandidateId(),
                candidate != null ? candidate.getName() : null,
                candidate != null ? candidate.getEmail() : null,
                savedInterview.getRound(),
                previousDateTime,
                savedInterview.getDateTime(),
                savedInterview.getDurationMinutes(),
                snapshotInterviewerUsernames(savedInterview),
                savedInterview.getMeetingLink(),
                savedInterview.getLocation(),
                savedInterview.getNotes(),
                changedBy,
                trimToNull(request.rescheduleReason())
        ));

        return InterviewResponse.from(
                savedInterview,
                candidate != null ? candidate.getName() : null
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional
    public InterviewResponse cancelInterview(String interviewId, CancelInterviewRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new ValidationException("Interview is already cancelled");
        }
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new ValidationException("Completed interviews cannot be cancelled");
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        String reason = request == null ? null : trimToNull(request.reason());
        String previousNotes = trimToNull(interview.getNotes());
        String cancellationNote = reason == null ? "Interview cancelled" : "Interview cancelled. Reason: " + reason;
        interview.setNotes(limitNotesLength(previousNotes == null
                ? cancellationNote
                : previousNotes + System.lineSeparator() + cancellationNote));
        String changedBy = resolveAuthenticatedUser();
        saveInterviewHistory(savedHistory(
                interview.getId(),
                InterviewActionType.CANCELLED,
                cancellationNote,
                changedBy
        ));

        Interview savedInterview = interviewRepository.save(interview);

        Candidate candidate = candidateRepository.findByIdAndDeletedFalse(savedInterview.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + savedInterview.getCandidateId()));
        RecruitmentStage rollbackStage = resolveRollbackStage(savedInterview.getRound());
        candidate.setStage(rollbackStage);
        candidateRepository.save(candidate);

        StageHistory stageHistory = new StageHistory();
        stageHistory.setCandidateId(candidate.getId());
        stageHistory.setStage(rollbackStage);
        stageHistory.setChangedAt(LocalDateTime.now());
        stageHistory.setChangedBy(changedBy);
        stageHistoryRepository.save(stageHistory);

        applicationEventPublisher.publishEvent(new InterviewCancelledEvent(
                savedInterview.getId(),
                savedInterview.getCandidateId(),
                candidate.getName(),
                candidate.getEmail(),
                savedInterview.getRound(),
                savedInterview.getDateTime(),
                savedInterview.getDurationMinutes(),
                snapshotInterviewerUsernames(savedInterview),
                savedInterview.getMeetingLink(),
                savedInterview.getLocation(),
                reason,
                changedBy
        ));

        return InterviewResponse.from(savedInterview, candidate.getName());
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

    private void ensureNoConflict(
            List<String> interviewerUsernames,
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd
    ) {
        ensureNoConflict(interviewerUsernames, requestedStart, requestedEnd, null);
    }

    private void ensureNoConflict(
            List<String> interviewerUsernames,
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd,
            String excludeInterviewId
    ) {
        LocalDateTime windowStart = requestedStart.minusHours(4);
        LocalDateTime windowEnd = requestedEnd.plusHours(4);
        List<Interview> nearbyScheduledInterviews = interviewRepository.findByStatusInAndDateTimeBetween(
                List.of(InterviewStatus.SCHEDULED), windowStart, windowEnd
        );

        Set<String> requested = new HashSet<>(interviewerUsernames);
        for (Interview existing : nearbyScheduledInterviews) {
            if (excludeInterviewId != null && excludeInterviewId.equals(existing.getId())) {
                continue;
            }
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

    private String buildRescheduleHistory(
            String changedBy,
            LocalDateTime previousDateTime,
            Integer previousDuration,
            List<String> previousInterviewers,
            LocalDateTime newDateTime,
            Integer newDuration,
            List<String> newInterviewers,
            String reason
    ) {
        String normalizedReason = trimToNull(reason);
        String reasonPart = normalizedReason == null ? "" : " reason='" + normalizedReason + "'";
        return "[rescheduled at %s by %s from %s/%d mins/%s to %s/%d mins/%s%s]"
                .formatted(
                        LocalDateTime.now(),
                        changedBy,
                        previousDateTime,
                        previousDuration,
                        previousInterviewers,
                        newDateTime,
                        newDuration,
                        newInterviewers,
                        reasonPart
                );
    }

    private String mergeNotesWithHistory(String requestNotes, String existingNotes, String historyLine) {
        String normalizedRequestNotes = trimToNull(requestNotes);
        String normalizedExistingNotes = trimToNull(existingNotes);
        String baseNotes = normalizedRequestNotes != null ? normalizedRequestNotes : normalizedExistingNotes;
        if (baseNotes == null) {
            return historyLine;
        }
        if (Objects.equals(baseNotes, historyLine)) {
            return baseNotes;
        }
        return limitNotesLength(baseNotes + System.lineSeparator() + historyLine);
    }

    private String limitNotesLength(String notes) {
        if (notes == null || notes.length() <= MAX_NOTES_LENGTH) {
            return notes;
        }
        return notes.substring(notes.length() - MAX_NOTES_LENGTH);
    }

    private RecruitmentStage resolveRollbackStage(InterviewRound round) {
        return switch (round) {
            case ROUND_1 -> RecruitmentStage.SHORTLISTED;
            case ROUND_2 -> RecruitmentStage.R1_CLEARED;
        };
    }

    private InterviewHistory savedHistory(String interviewId, InterviewActionType actionType, String details, String changedBy) {
        InterviewHistory history = new InterviewHistory();
        history.setInterviewId(interviewId);
        history.setActionType(actionType);
        history.setDetails(details);
        history.setChangedBy(changedBy);
        history.setChangedAt(LocalDateTime.now());
        return history;
    }

    private void saveInterviewHistory(InterviewHistory history) {
        interviewHistoryRepository.save(history);
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

    private static List<String> snapshotInterviewerUsernames(Interview interview) {
        List<String> usernames = interview.getInterviewerUsernames();
        return usernames == null || usernames.isEmpty() ? List.of() : List.copyOf(usernames);
    }
}
