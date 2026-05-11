package com.rts.modules.feedback.application;

import com.rts.modules.candidate.application.CandidateEvalPort;
import com.rts.modules.feedback.api.dto.FeedbackResponse;
import com.rts.modules.feedback.api.dto.SubmitFeedbackRequest;
import com.rts.modules.feedback.domain.Feedback;
import com.rts.modules.feedback.persistence.FeedbackRepository;
import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewStatus;
import com.rts.modules.interview.persistence.InterviewRepository;
import com.rts.shared.events.FeedbackSubmittedEvent;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class FeedbackService {

    private static final int EDIT_WINDOW_HOURS = 24;

    private final FeedbackRepository feedbackRepository;
    private final InterviewRepository interviewRepository;
    private final CandidateEvalPort candidateEvalPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            InterviewRepository interviewRepository,
            CandidateEvalPort candidateEvalPort,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.feedbackRepository = feedbackRepository;
        this.interviewRepository = interviewRepository;
        this.candidateEvalPort = candidateEvalPort;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER', 'INTERVIEWER')")
    @Transactional
    public FeedbackResponse submit(SubmitFeedbackRequest request) {
        String interviewId = request.interviewId().trim();
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new ValidationException("Feedback cannot be submitted for a cancelled interview");
        }
        if (interview.getStatus() == InterviewStatus.SCHEDULED) {
            LocalDateTime end = interview.getDateTime().plusMinutes(interview.getDurationMinutes());
            if (LocalDateTime.now().isBefore(end)) {
                throw new ValidationException("Feedback can only be submitted after the interview time window has ended");
            }
        }

        String username = resolveAuthenticatedUsername();
        assertCanSubmitForInterview(interview, username);

        Optional<Feedback> existingOpt =
                feedbackRepository.findByInterviewIdAndSubmittedByUsernameIgnoreCaseAndDeletedFalse(interviewId, username);

        LocalDateTime now = LocalDateTime.now();
        Feedback entity;
        if (existingOpt.isPresent()) {
            Feedback existing = existingOpt.get();
            if (existing.getSubmittedAt().plusHours(EDIT_WINDOW_HOURS).isBefore(now)) {
                throw new ValidationException("Feedback can no longer be edited after 24 hours from first submission");
            }
            entity = existing;
        } else {
            entity = new Feedback();
            entity.setInterviewId(interviewId);
            entity.setCandidateId(interview.getCandidateId());
            entity.setSubmittedByUsername(username.toLowerCase(Locale.ROOT));
            entity.setSubmittedAt(now);
        }

        entity.setTechnicalRating(request.technicalRating());
        entity.setCommunicationRating(request.communicationRating());
        entity.setProblemSolvingRating(request.problemSolvingRating());
        entity.setLeadershipRating(request.leadershipRating());
        entity.setCultureRating(request.cultureRating());
        entity.setRecommendation(request.recommendation());
        entity.setComments(trimToNull(request.comments()));

        Feedback saved = feedbackRepository.save(entity);
        refreshCandidateEvalScore(interview.getCandidateId());

        applicationEventPublisher.publishEvent(new FeedbackSubmittedEvent(
                saved.getId(),
                saved.getInterviewId(),
                saved.getCandidateId(),
                saved.getSubmittedByUsername(),
                saved.getRecommendation(),
                saved.getSubmittedAt()
        ));

        return FeedbackResponse.from(saved);
    }

    private void assertCanSubmitForInterview(Interview interview, String username) {
        String normalized = username.toLowerCase(Locale.ROOT);
        boolean isPrivileged = hasAnyRole("ROLE_ADMIN", "ROLE_HR_MANAGER", "ROLE_RECRUITER");
        if (isPrivileged) {
            return;
        }
        List<String> panel = interview.getInterviewerUsernames();
        boolean onPanel = panel.stream().map(u -> u.toLowerCase(Locale.ROOT)).anyMatch(normalized::equals);
        if (!onPanel) {
            throw new ValidationException("Interviewers can only submit feedback for interviews they are assigned to");
        }
    }

    private boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (String role : roles) {
            if (authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()))) {
                return true;
            }
        }
        return false;
    }

    private void refreshCandidateEvalScore(String candidateId) {
        Double avg = feedbackRepository.averagePerFeedbackScore(candidateId);
        candidateEvalPort.applyEvalScore(candidateId, avg);
    }

    private String resolveAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "system";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String s && !s.isBlank()) {
            return s;
        }
        String name = authentication.getName();
        return name == null || name.isBlank() ? "system" : name;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
