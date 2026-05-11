package com.rts.modules.feedback.application;

import com.rts.modules.candidate.application.CandidateEvalPort;
import com.rts.modules.feedback.api.dto.SubmitFeedbackRequest;
import com.rts.modules.feedback.domain.Feedback;
import com.rts.modules.feedback.domain.FeedbackRecommendation;
import com.rts.modules.feedback.persistence.FeedbackRepository;
import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewRound;
import com.rts.modules.interview.domain.InterviewStatus;
import com.rts.modules.interview.persistence.InterviewRepository;
import com.rts.shared.events.FeedbackSubmittedEvent;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private CandidateEvalPort candidateEvalPort;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        feedbackService = new FeedbackService(
                feedbackRepository,
                interviewRepository,
                candidateEvalPort,
                applicationEventPublisher
        );
    }

    @Test
    void submitShouldPersistFeedbackAndRefreshEvalScore() {
        LocalDateTime pastStart = LocalDateTime.now().minusDays(1);
        Interview interview = interview();
        interview.setDateTime(pastStart);
        interview.setStatus(InterviewStatus.SCHEDULED);

        when(interviewRepository.findById("int-1")).thenReturn(Optional.of(interview));
        when(feedbackRepository.findByInterviewIdAndSubmittedByUsernameIgnoreCaseAndDeletedFalse("int-1", "alice"))
                .thenReturn(Optional.empty());
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback f = invocation.getArgument(0);
            f.setId("fb-1");
            return f;
        });
        when(feedbackRepository.averagePerFeedbackScore("cand-1")).thenReturn(4.2);

        authenticate("alice", "ROLE_INTERVIEWER");

        SubmitFeedbackRequest request = requestAllFives("int-1");
        feedbackService.submit(request);

        ArgumentCaptor<Feedback> fbCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(fbCaptor.capture());
        Feedback saved = fbCaptor.getValue();
        assertThat(saved.getTechnicalRating()).isEqualTo(5);
        assertThat(saved.getSubmittedByUsername()).isEqualTo("alice");
        assertThat(saved.getCandidateId()).isEqualTo("cand-1");

        verify(candidateEvalPort).applyEvalScore("cand-1", 4.2);
        verify(applicationEventPublisher).publishEvent(any(FeedbackSubmittedEvent.class));
    }

    @Test
    void submitShouldAllowUpdateWithin24Hours() {
        LocalDateTime pastStart = LocalDateTime.now().minusDays(2);
        Interview interview = interview();
        interview.setDateTime(pastStart);

        Feedback existing = new Feedback();
        existing.setId("fb-old");
        existing.setInterviewId("int-1");
        existing.setCandidateId("cand-1");
        existing.setSubmittedByUsername("alice");
        existing.setSubmittedAt(LocalDateTime.now().minusHours(1));
        existing.setTechnicalRating(2);

        when(interviewRepository.findById("int-1")).thenReturn(Optional.of(interview));
        when(feedbackRepository.findByInterviewIdAndSubmittedByUsernameIgnoreCaseAndDeletedFalse("int-1", "alice"))
                .thenReturn(Optional.of(existing));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(feedbackRepository.averagePerFeedbackScore("cand-1")).thenReturn(5.0);

        authenticate("alice", "ROLE_INTERVIEWER");

        feedbackService.submit(requestAllFives("int-1"));

        assertThat(existing.getTechnicalRating()).isEqualTo(5);
        verify(candidateEvalPort).applyEvalScore("cand-1", 5.0);
    }

    @Test
    void submitShouldRejectUpdateAfter24Hours() {
        LocalDateTime pastStart = LocalDateTime.now().minusDays(3);
        Interview interview = interview();
        interview.setDateTime(pastStart);

        Feedback existing = new Feedback();
        existing.setSubmittedAt(LocalDateTime.now().minusHours(25));

        when(interviewRepository.findById("int-1")).thenReturn(Optional.of(interview));
        when(feedbackRepository.findByInterviewIdAndSubmittedByUsernameIgnoreCaseAndDeletedFalse("int-1", "alice"))
                .thenReturn(Optional.of(existing));

        authenticate("alice", "ROLE_INTERVIEWER");

        assertThatThrownBy(() -> feedbackService.submit(requestAllFives("int-1")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("24 hours");

        verify(feedbackRepository, never()).save(any());
        verify(candidateEvalPort, never()).applyEvalScore(any(), any());
    }

    @Test
    void interviewerNotOnPanelShouldBeRejected() {
        Interview interview = interview();
        interview.setDateTime(LocalDateTime.now().minusDays(1));

        when(interviewRepository.findById("int-1")).thenReturn(Optional.of(interview));

        authenticate("stranger", "ROLE_INTERVIEWER");

        assertThatThrownBy(() -> feedbackService.submit(requestAllFives("int-1")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("assigned");

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void recruiterCanSubmitWithoutBeingOnPanel() {
        Interview interview = interview();
        interview.setDateTime(LocalDateTime.now().minusDays(1));

        when(interviewRepository.findById("int-1")).thenReturn(Optional.of(interview));
        when(feedbackRepository.findByInterviewIdAndSubmittedByUsernameIgnoreCaseAndDeletedFalse("int-1", "rec"))
                .thenReturn(Optional.empty());
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback f = invocation.getArgument(0);
            f.setId("fb-2");
            return f;
        });
        when(feedbackRepository.averagePerFeedbackScore("cand-1")).thenReturn(3.0);

        authenticate("rec", "ROLE_RECRUITER");

        feedbackService.submit(requestAllFives("int-1"));

        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    void submitShouldRejectCancelledInterview() {
        Interview interview = interview();
        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setDateTime(LocalDateTime.now().minusDays(1));

        when(interviewRepository.findById("int-1")).thenReturn(Optional.of(interview));
        authenticate("alice", "ROLE_INTERVIEWER");

        assertThatThrownBy(() -> feedbackService.submit(requestAllFives("int-1")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void submitShouldRejectBeforeInterviewWindowEnds() {
        Interview interview = interview();
        interview.setDateTime(LocalDateTime.now().plusHours(2));
        interview.setDurationMinutes(60);

        when(interviewRepository.findById("int-1")).thenReturn(Optional.of(interview));
        authenticate("alice", "ROLE_INTERVIEWER");

        assertThatThrownBy(() -> feedbackService.submit(requestAllFives("int-1")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("after the interview");
    }

    @Test
    void submitShouldFailWhenInterviewMissing() {
        when(interviewRepository.findById("missing")).thenReturn(Optional.empty());
        authenticate("alice", "ROLE_RECRUITER");

        assertThatThrownBy(() -> feedbackService.submit(requestAllFives("missing")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Interview interview() {
        Interview i = new Interview();
        i.setId("int-1");
        i.setCandidateId("cand-1");
        i.setRound(InterviewRound.ROUND_1);
        i.setStatus(InterviewStatus.SCHEDULED);
        i.setDurationMinutes(45);
        i.setDateTime(LocalDateTime.now().minusDays(1));
        i.setInterviewerUsernames(List.of("alice"));
        return i;
    }

    private static SubmitFeedbackRequest requestAllFives(String interviewId) {
        return new SubmitFeedbackRequest(
                interviewId,
                5,
                5,
                5,
                5,
                5,
                FeedbackRecommendation.PROCEED,
                "Solid candidate"
        );
    }

    private static void authenticate(String user, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                )
        );
    }
}
