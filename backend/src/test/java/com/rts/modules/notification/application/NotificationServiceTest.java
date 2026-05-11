package com.rts.modules.notification.application;

import com.rts.modules.feedback.persistence.FeedbackRepository;
import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewRound;
import com.rts.modules.interview.domain.InterviewStatus;
import com.rts.modules.interview.persistence.InterviewRepository;
import com.rts.modules.notification.domain.Notification;
import com.rts.modules.notification.domain.NotificationType;
import com.rts.modules.notification.persistence.NotificationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
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

    @Captor
    private ArgumentCaptor<List<Notification>> notificationsCaptor;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                interviewRepository,
                feedbackRepository
        );
    }

    @Test
    void checkPendingFeedbackShouldCreateNotificationsForOverdueInterviews() {
        Interview overdueInterview = buildInterview(
                "int-1", "cand-1", InterviewRound.ROUND_1,
                LocalDateTime.now().minusHours(48), 60,
                List.of("alice", "bob")
        );

        when(interviewRepository.findInterviewsScheduledBetween(any(), any(), any()))
                .thenReturn(List.of(overdueInterview));
        when(feedbackRepository.findInterviewIdsWithFeedback(List.of("int-1")))
                .thenReturn(Collections.emptyList());

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

        notificationService.checkPendingFeedback();

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        List<Notification> saved = notificationsCaptor.getValue();

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getUserId()).isEqualTo("alice");
        assertThat(saved.get(0).getMessage()).contains("cand-a");
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
