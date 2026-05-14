package com.rts.modules.notification.application;

import com.rts.infrastructure.mail.EmailPort;
import com.rts.infrastructure.notification.UserEmailResolverPort;
import com.rts.modules.feedback.persistence.FeedbackRepository;
import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewRound;
import com.rts.modules.interview.domain.InterviewStatus;
import com.rts.modules.interview.persistence.InterviewRepository;
import com.rts.modules.notification.domain.Notification;
import com.rts.modules.notification.domain.NotificationType;
import com.rts.modules.notification.persistence.NotificationRepository;
import com.rts.shared.events.CandidateRegisteredEvent;
import com.rts.shared.events.CandidateStageChangedEvent;
import com.rts.shared.events.InterviewCancelledEvent;
import com.rts.shared.events.InterviewRescheduledEvent;
import com.rts.shared.events.InterviewScheduledEvent;
import com.rts.shared.kernel.RecruitmentStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private UserEmailResolverPort userEmailResolver;

    @Mock
    private EmailPort emailPort;

    @Captor
    private ArgumentCaptor<List<Notification>> notificationsCaptor;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                interviewRepository,
                feedbackRepository,
                userEmailResolver,
                emailPort
        );
    }

    @Test
    void checkPendingFeedbackShouldCreateNotificationsAndSendEmails() {
        Interview overdueInterview = buildInterview(
                "int-1", "cand-1", InterviewRound.ROUND_1,
                LocalDateTime.now().minusHours(48), 60,
                List.of("alice", "bob")
        );

        when(interviewRepository.findInterviewsScheduledBetween(any(), any(), any()))
                .thenReturn(List.of(overdueInterview));
        when(feedbackRepository.findInterviewIdsWithFeedback(List.of("int-1")))
                .thenReturn(Collections.emptyList());
        when(userEmailResolver.resolveEmails(List.of("alice", "bob")))
                .thenReturn(Map.of("alice", "alice@rts.com", "bob", "bob@rts.com"));

        notificationService.checkPendingFeedback();

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        List<Notification> saved = notificationsCaptor.getValue();

        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(n -> n.getType() == NotificationType.FEEDBACK_PENDING);
        assertThat(saved).allMatch(n -> !n.isRead());
        assertThat(saved.stream().map(Notification::getUserId).toList())
                .containsExactlyInAnyOrder("alice", "bob");
        assertThat(saved.get(0).getMessage()).contains("Feedback pending");
        assertThat(saved.get(0).getMessage()).contains("cand-1");

        verify(emailPort).send(eq("alice@rts.com"), contains("Feedback"), anyString());
        verify(emailPort).send(eq("bob@rts.com"), contains("Feedback"), anyString());
    }

    @Test
    void checkPendingFeedbackShouldSkipInterviewsWithExistingFeedback() {
        Interview interviewWithFeedback = buildInterview(
                "int-2", "cand-2", InterviewRound.ROUND_1,
                LocalDateTime.now().minusHours(48), 45,
                List.of("charlie")
        );

        when(interviewRepository.findInterviewsScheduledBetween(any(), any(), any()))
                .thenReturn(List.of(interviewWithFeedback));
        when(feedbackRepository.findInterviewIdsWithFeedback(List.of("int-2")))
                .thenReturn(List.of("int-2"));

        notificationService.checkPendingFeedback();

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    void checkPendingFeedbackShouldDoNothingWhenNoInterviewsFound() {
        when(interviewRepository.findInterviewsScheduledBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        notificationService.checkPendingFeedback();

        verify(feedbackRepository, never()).findInterviewIdsWithFeedback(anyCollection());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    void checkPendingFeedbackShouldSkipInterviewsNotYet24HoursOverdue() {
        Interview recentInterview = buildInterview(
                "int-3", "cand-3", InterviewRound.ROUND_2,
                LocalDateTime.now().minusHours(20), 60,
                List.of("dave")
        );

        when(interviewRepository.findInterviewsScheduledBetween(any(), any(), any()))
                .thenReturn(List.of(recentInterview));
        when(feedbackRepository.findInterviewIdsWithFeedback(List.of("int-3")))
                .thenReturn(Collections.emptyList());

        notificationService.checkPendingFeedback();

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    void checkPendingFeedbackShouldHandleMixedScenarios() {
        Interview overdueNoFeedback = buildInterview(
                "int-a", "cand-a", InterviewRound.ROUND_1,
                LocalDateTime.now().minusHours(50), 45,
                List.of("alice")
        );
        Interview overdueWithFeedback = buildInterview(
                "int-b", "cand-b", InterviewRound.ROUND_2,
                LocalDateTime.now().minusHours(50), 60,
                List.of("bob")
        );

        when(interviewRepository.findInterviewsScheduledBetween(any(), any(), any()))
                .thenReturn(List.of(overdueNoFeedback, overdueWithFeedback));
        when(feedbackRepository.findInterviewIdsWithFeedback(List.of("int-a", "int-b")))
                .thenReturn(List.of("int-b"));
        when(userEmailResolver.resolveEmails(List.of("alice")))
                .thenReturn(Map.of("alice", "alice@rts.com"));

        notificationService.checkPendingFeedback();

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        List<Notification> saved = notificationsCaptor.getValue();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getUserId()).isEqualTo("alice");
        assertThat(saved.get(0).getMessage()).contains("cand-a");

        verify(emailPort).send(eq("alice@rts.com"), contains("Feedback"), anyString());
    }

    @Test
    void handleInterviewScheduledShouldSendRichEmailsToInterviewersAndCandidate() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(2);

        InterviewScheduledEvent event = new InterviewScheduledEvent(
                "int-100", "cand-100", "John Doe", "john@example.com",
                InterviewRound.ROUND_1, scheduledAt, 45,
                List.of("alice", "bob"),
                "https://meet.google.com/abc-xyz", null,
                "Please prepare questions", "recruiter.user"
        );

        when(userEmailResolver.resolveEmails(List.of("alice", "bob")))
                .thenReturn(Map.of("alice", "alice@rts.com", "bob", "bob@rts.com"));

        notificationService.handleInterviewScheduled(event);

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        List<Notification> saved = notificationsCaptor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(n -> n.getType() == NotificationType.INTERVIEW_SCHEDULED);
        assertThat(saved.get(0).getMessage()).contains("John Doe");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        // 2 in-app notification emails + 2 rich interviewer emails + 1 candidate email = 5
        verify(emailPort, times(5)).send(anyString(), anyString(), bodyCaptor.capture());

        List<String> allBodies = bodyCaptor.getAllValues();
        List<String> richBodies = allBodies.stream()
                .filter(b -> b.contains("Interview Details"))
                .toList();
        assertThat(richBodies).hasSize(3);
        assertThat(richBodies.get(0)).contains("John Doe");
        assertThat(richBodies.get(0)).contains("https://meet.google.com/abc-xyz");
        assertThat(richBodies.get(0)).contains("45 minutes");
        assertThat(richBodies.get(0)).contains("alice");
        assertThat(richBodies.get(0)).contains("Please prepare questions");
    }

    @Test
    void handleInterviewScheduledShouldSkipCandidateEmailWhenMissing() {
        InterviewScheduledEvent event = new InterviewScheduledEvent(
                "int-101", "cand-101", "Jane Smith", null,
                InterviewRound.ROUND_2, LocalDateTime.now().plusDays(1), 60,
                List.of("charlie"),
                null, "Room 5B", null, "recruiter.user"
        );

        when(userEmailResolver.resolveEmails(List.of("charlie")))
                .thenReturn(Map.of("charlie", "charlie@rts.com"));

        notificationService.handleInterviewScheduled(event);

        verify(notificationRepository).saveAll(any());
        // 1 in-app notification email + 1 rich interviewer email = 2 (no candidate email)
        verify(emailPort, times(2)).send(anyString(), anyString(), anyString());
        verify(emailPort, never()).send(eq((String) null), anyString(), anyString());
    }

    @Test
    void handleInterviewRescheduledShouldSendRichEmailsToInterviewersAndCandidate() {
        LocalDateTime oldTime = LocalDateTime.now().plusDays(1);
        LocalDateTime newTime = oldTime.plusDays(2);

        InterviewRescheduledEvent event = new InterviewRescheduledEvent(
                "int-200", "cand-200", "Mike Johnson", "mike@example.com",
                InterviewRound.ROUND_1, oldTime, newTime, 60,
                List.of("dave"),
                "https://meet.google.com/new-link", null,
                "Updated notes", "recruiter.user", "Panel unavailable"
        );

        when(userEmailResolver.resolveEmails(List.of("dave")))
                .thenReturn(Map.of("dave", "dave@rts.com"));

        notificationService.handleInterviewRescheduled(event);

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        assertThat(notificationsCaptor.getValue()).hasSize(1);
        assertThat(notificationsCaptor.getValue().get(0).getMessage()).contains("Mike Johnson");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        // 1 in-app notification email + 1 rich interviewer email + 1 candidate email = 3
        verify(emailPort, times(3)).send(anyString(), anyString(), bodyCaptor.capture());

        List<String> richBodies = bodyCaptor.getAllValues().stream()
                .filter(b -> b.contains("Interview Rescheduled"))
                .toList();
        assertThat(richBodies).hasSize(2);
        assertThat(richBodies.get(0)).contains("Mike Johnson");
        assertThat(richBodies.get(0)).contains("Panel unavailable");
        assertThat(richBodies.get(0)).contains("https://meet.google.com/new-link");
    }

    @Test
    void handleInterviewRescheduledShouldClipInAppNotificationWhenReasonIsVeryLong() {
        LocalDateTime oldTime = LocalDateTime.now().plusDays(1);
        LocalDateTime newTime = oldTime.plusDays(2);
        String longReason = "R".repeat(600);

        InterviewRescheduledEvent event = new InterviewRescheduledEvent(
                "int-201", "cand-200", "Mike Johnson", "mike@example.com",
                InterviewRound.ROUND_1, oldTime, newTime, 60,
                List.of("dave"),
                "https://meet.google.com/new-link", null,
                "Updated notes", "recruiter.user", longReason
        );

        when(userEmailResolver.resolveEmails(List.of("dave")))
                .thenReturn(Map.of("dave", "dave@rts.com"));

        notificationService.handleInterviewRescheduled(event);

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        assertThat(notificationsCaptor.getValue()).hasSize(1);
        assertThat(notificationsCaptor.getValue().get(0).getMessage()).hasSize(500);
    }

    @Test
    void handleInterviewCancelledShouldSendEmailsToInterviewersAndCandidate() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(3);

        InterviewCancelledEvent event = new InterviewCancelledEvent(
                "int-300", "cand-300", "Sarah Connor", "sarah@example.com",
                InterviewRound.ROUND_1, scheduledAt, 45,
                List.of("alice", "bob"),
                "https://meet.google.com/cancel-test", null,
                "Panel unavailable", "recruiter.user"
        );

        when(userEmailResolver.resolveEmails(List.of("alice", "bob")))
                .thenReturn(Map.of("alice", "alice@rts.com", "bob", "bob@rts.com"));

        notificationService.handleInterviewCancelled(event);

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        List<Notification> saved = notificationsCaptor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(n -> n.getType() == NotificationType.INTERVIEW_CANCELLED);
        assertThat(saved.get(0).getMessage()).contains("cancelled");
        assertThat(saved.get(0).getMessage()).contains("Sarah Connor");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        // 2 in-app notification emails + 2 rich interviewer emails + 1 candidate email = 5
        verify(emailPort, times(5)).send(anyString(), anyString(), bodyCaptor.capture());

        List<String> richBodies = bodyCaptor.getAllValues().stream()
                .filter(b -> b.contains("Interview Cancelled"))
                .toList();
        assertThat(richBodies).hasSize(3);
        assertThat(richBodies.get(0)).contains("Sarah Connor");
        assertThat(richBodies.get(0)).contains("Panel unavailable");
        assertThat(richBodies.get(0)).contains("rescheduled");
    }

    @Test
    void handleInterviewCancelledShouldSkipCandidateEmailWhenMissing() {
        InterviewCancelledEvent event = new InterviewCancelledEvent(
                "int-301", "cand-301", "No Email Person", null,
                InterviewRound.ROUND_2, LocalDateTime.now().plusDays(1), 60,
                List.of("charlie"),
                null, "Room 5B",
                null, "recruiter.user"
        );

        when(userEmailResolver.resolveEmails(List.of("charlie")))
                .thenReturn(Map.of("charlie", "charlie@rts.com"));

        notificationService.handleInterviewCancelled(event);

        verify(notificationRepository).saveAll(any());
        // 1 in-app notification email + 1 rich interviewer email = 2 (no candidate email)
        verify(emailPort, times(2)).send(anyString(), anyString(), anyString());
        verify(emailPort, never()).send(eq((String) null), anyString(), anyString());
    }

    @Test
    void handleInterviewCancelledShouldWorkWithoutReason() {
        InterviewCancelledEvent event = new InterviewCancelledEvent(
                "int-302", "cand-302", "Jane Smith", "jane@example.com",
                InterviewRound.ROUND_1, LocalDateTime.now().plusDays(2), 30,
                List.of("dave"),
                "https://meet.google.com/test", null,
                null, "admin.user"
        );

        when(userEmailResolver.resolveEmails(List.of("dave")))
                .thenReturn(Map.of("dave", "dave@rts.com"));

        notificationService.handleInterviewCancelled(event);

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        List<Notification> saved = notificationsCaptor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getMessage()).contains("cancelled");
        assertThat(saved.get(0).getMessage()).doesNotContain("Reason:");

        // 1 notification email + 1 rich interviewer email + 1 candidate email = 3
        verify(emailPort, times(3)).send(anyString(), anyString(), anyString());
    }

    // ── Candidate Registration Confirmation Tests ─────────────────────

    @Test
    void handleCandidateRegisteredShouldSendConfirmationEmail() {
        CandidateRegisteredEvent event = new CandidateRegisteredEvent(
                "cand-reg-1", "Alice Smith", "alice@example.com", "Software Engineer"
        );

        notificationService.handleCandidateRegistered(event);

        verify(emailPort).send(
                eq("alice@example.com"),
                contains("Application Received"),
                contains("Software Engineer")
        );
    }

    @Test
    void handleCandidateRegisteredShouldSkipWhenEmailIsNull() {
        CandidateRegisteredEvent event = new CandidateRegisteredEvent(
                "cand-reg-2", "Bob Jones", null, "Designer"
        );

        notificationService.handleCandidateRegistered(event);

        verify(emailPort, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void handleCandidateRegisteredShouldSkipWhenEmailIsBlank() {
        CandidateRegisteredEvent event = new CandidateRegisteredEvent(
                "cand-reg-3", "Charlie", "   ", "QA Engineer"
        );

        notificationService.handleCandidateRegistered(event);

        verify(emailPort, never()).send(anyString(), anyString(), anyString());
    }

    // ── Candidate Stage Change Notification Tests ───────────────────────

    @Test
    void handleCandidateStageChangedShouldSendShortlistedEmail() {
        CandidateStageChangedEvent event = new CandidateStageChangedEvent(
                "cand-sc-1", "Alice Smith", "alice@example.com", "Backend Engineer",
                RecruitmentStage.SCREENING, RecruitmentStage.SHORTLISTED, "recruiter1"
        );

        notificationService.handleCandidateStageChanged(event);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailPort).send(eq("alice@example.com"), contains("Shortlisted"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains("shortlisted");
        assertThat(bodyCaptor.getValue()).contains("Backend Engineer");
    }

    @Test
    void handleCandidateStageChangedShouldSendRejectionEmail() {
        CandidateStageChangedEvent event = new CandidateStageChangedEvent(
                "cand-sc-2", "Bob Jones", "bob@example.com", "Frontend Developer",
                RecruitmentStage.R1_CLEARED, RecruitmentStage.REJECTED, "hr_manager"
        );

        notificationService.handleCandidateStageChanged(event);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailPort).send(eq("bob@example.com"), contains("Application Update"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains("move forward with other candidates");
    }

    @Test
    void handleCandidateStageChangedShouldSendHiredEmail() {
        CandidateStageChangedEvent event = new CandidateStageChangedEvent(
                "cand-sc-3", "Charlie Brown", "charlie@example.com", "DevOps Engineer",
                RecruitmentStage.OFFERED, RecruitmentStage.HIRED, "admin"
        );

        notificationService.handleCandidateStageChanged(event);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailPort).send(eq("charlie@example.com"), contains("Welcome Aboard"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains("thrilled to have you join");
    }

    @Test
    void handleCandidateStageChangedShouldSkipWhenEmailIsNull() {
        CandidateStageChangedEvent event = new CandidateStageChangedEvent(
                "cand-sc-4", "No Email", null, "Analyst",
                RecruitmentStage.APPLICATION_RECEIVED, RecruitmentStage.SCREENING, "recruiter"
        );

        notificationService.handleCandidateStageChanged(event);

        verify(emailPort, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void handleCandidateStageChangedShouldSendOfferedEmail() {
        CandidateStageChangedEvent event = new CandidateStageChangedEvent(
                "cand-sc-5", "Dana White", "dana@example.com", "Product Manager",
                RecruitmentStage.R2_CLEARED, RecruitmentStage.OFFERED, "hr_manager"
        );

        notificationService.handleCandidateStageChanged(event);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailPort).send(eq("dana@example.com"), contains("Offer Extended"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).contains("offer has been extended");
    }

    private static Interview buildInterview(String id, String candidateId, InterviewRound round,
                                             LocalDateTime dateTime, int durationMinutes,
                                             List<String> interviewers) {
        Interview interview = new Interview();
        interview.setId(id);
        interview.setCandidateId(candidateId);
        interview.setRound(round);
        interview.setDateTime(dateTime);
        interview.setDurationMinutes(durationMinutes);
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setInterviewerUsernames(interviewers);
        return interview;
    }

}
