package com.rts.modules.interview.application;

import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.domain.StageHistory;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.modules.candidate.persistence.StageHistoryRepository;
import com.rts.modules.interview.api.dto.InterviewResponse;
import com.rts.modules.interview.api.dto.CancelInterviewRequest;
import com.rts.modules.interview.api.dto.RescheduleInterviewRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private StageHistoryRepository stageHistoryRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private InterviewHistoryRepository interviewHistoryRepository;

    private InterviewService interviewService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        interviewService = new InterviewService(
                interviewRepository,
                candidateRepository,
                stageHistoryRepository,
                interviewHistoryRepository,
                applicationEventPublisher
        );
    }

    @Test
    void scheduleRoundOneShouldAutoUpdateStageAndPublishNotificationEvent() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        ScheduleRoundOneInterviewRequest request = new ScheduleRoundOneInterviewRequest(
                "candidate-1",
                now,
                45,
                "https://meet.google.com/r1-test",
                List.of("interviewer.a", "interviewer.b"),
                "Round 1 discussion"
        );

        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setStage(RecruitmentStage.SHORTLISTED);

        when(candidateRepository.findByIdAndDeletedFalse("candidate-1")).thenReturn(Optional.of(candidate));
        when(interviewRepository.findByStatusInAndDateTimeBetween(anyCollection(), any(), any())).thenReturn(List.of());
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> {
            Interview interview = invocation.getArgument(0);
            interview.setId("interview-1");
            return interview;
        });
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("recruiter.user", null, List.of())
        );

        InterviewResponse response = interviewService.scheduleRoundOne(request);

        assertThat(response.round()).isEqualTo(InterviewRound.ROUND_1);
        assertThat(candidate.getStage()).isEqualTo(RecruitmentStage.R1_SCHEDULED);

        ArgumentCaptor<StageHistory> historyCaptor = ArgumentCaptor.forClass(StageHistory.class);
        verify(stageHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStage()).isEqualTo(RecruitmentStage.R1_SCHEDULED);
        assertThat(historyCaptor.getValue().getChangedBy()).isEqualTo("recruiter.user");

        ArgumentCaptor<InterviewScheduledEvent> eventCaptor = ArgumentCaptor.forClass(InterviewScheduledEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().round()).isEqualTo(InterviewRound.ROUND_1);
        assertThat(eventCaptor.getValue().candidateId()).isEqualTo("candidate-1");
    }

    @Test
    void scheduleRoundOneShouldFailWhenInterviewerConflictExists() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withMinute(0);
        ScheduleRoundOneInterviewRequest request = new ScheduleRoundOneInterviewRequest(
                "candidate-1",
                start.plusMinutes(30),
                45,
                "https://meet.google.com/r1-conflict",
                List.of("interviewer.a"),
                null
        );

        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setStage(RecruitmentStage.SHORTLISTED);

        Interview existing = new Interview();
        existing.setId("existing-1");
        existing.setDateTime(start);
        existing.setDurationMinutes(60);
        existing.setStatus(InterviewStatus.SCHEDULED);
        existing.setInterviewerUsernames(List.of("interviewer.a"));

        when(candidateRepository.findByIdAndDeletedFalse("candidate-1")).thenReturn(Optional.of(candidate));
        when(interviewRepository.findByStatusInAndDateTimeBetween(anyCollection(), any(), any()))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> interviewService.scheduleRoundOne(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Scheduling conflict detected for one or more interviewers");

        verify(interviewRepository, never()).save(any(Interview.class));
        verify(stageHistoryRepository, never()).save(any(StageHistory.class));
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void scheduleRoundTwoShouldFailWhenCandidateMissing() {
        ScheduleRoundTwoInterviewRequest request = new ScheduleRoundTwoInterviewRequest(
                "missing-id",
                LocalDateTime.now().plusDays(2),
                30,
                "Room X",
                List.of("interviewer.b"),
                null
        );

        when(candidateRepository.findByIdAndDeletedFalse("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.scheduleRoundTwo(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Candidate not found: missing-id");

        verify(interviewRepository, never()).save(any(Interview.class));
    }

    @Test
    void scheduleRoundTwoShouldFailWhenCandidateNotR1Cleared() {
        ScheduleRoundTwoInterviewRequest request = new ScheduleRoundTwoInterviewRequest(
                "candidate-2",
                LocalDateTime.now().plusDays(2),
                30,
                "Conference Room A",
                List.of("interviewer.b"),
                null
        );

        Candidate candidate = new Candidate();
        candidate.setId("candidate-2");
        candidate.setStage(RecruitmentStage.R1_SCHEDULED);

        when(candidateRepository.findByIdAndDeletedFalse("candidate-2")).thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> interviewService.scheduleRoundTwo(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Round 2 can only be scheduled for candidates in stage: R1_CLEARED");

        verify(interviewRepository, never()).save(any(Interview.class));
    }

    @Test
    void scheduleRoundTwoShouldRequireLocation() {
        ScheduleRoundTwoInterviewRequest request = new ScheduleRoundTwoInterviewRequest(
                "candidate-3",
                LocalDateTime.now().plusDays(2),
                30,
                "   ",
                List.of("interviewer.c"),
                "Onsite panel"
        );

        Candidate candidate = new Candidate();
        candidate.setId("candidate-3");
        candidate.setStage(RecruitmentStage.R1_CLEARED);
        when(candidateRepository.findByIdAndDeletedFalse("candidate-3")).thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> interviewService.scheduleRoundTwo(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Location is required");
    }

    @Test
    void scheduleRoundTwoShouldValidateConflictsAndPublishNotificationEvent() {
        LocalDateTime start = LocalDateTime.now().plusDays(3).withMinute(0);
        ScheduleRoundTwoInterviewRequest request = new ScheduleRoundTwoInterviewRequest(
                "candidate-4",
                start.plusHours(1),
                60,
                "HQ Room 201",
                List.of("interviewer.d", "interviewer.e"),
                "Final round"
        );

        Candidate candidate = new Candidate();
        candidate.setId("candidate-4");
        candidate.setStage(RecruitmentStage.R1_CLEARED);

        when(candidateRepository.findByIdAndDeletedFalse("candidate-4")).thenReturn(Optional.of(candidate));
        when(interviewRepository.findByStatusInAndDateTimeBetween(anyCollection(), any(), any())).thenReturn(List.of());
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> {
            Interview interview = invocation.getArgument(0);
            interview.setId("interview-r2");
            return interview;
        });
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("hr.user", null, List.of())
        );

        InterviewResponse response = interviewService.scheduleRoundTwo(request);

        assertThat(response.round()).isEqualTo(InterviewRound.ROUND_2);
        assertThat(response.location()).isEqualTo("HQ Room 201");
        assertThat(candidate.getStage()).isEqualTo(RecruitmentStage.R2_SCHEDULED);

        ArgumentCaptor<InterviewScheduledEvent> eventCaptor = ArgumentCaptor.forClass(InterviewScheduledEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().round()).isEqualTo(InterviewRound.ROUND_2);
        assertThat(eventCaptor.getValue().interviewerUsernames()).containsExactly("interviewer.d", "interviewer.e");

        verify(interviewRepository).findByStatusInAndDateTimeBetween(anyList(), any(), any());
        verify(stageHistoryRepository).save(any(StageHistory.class));
    }

    @Test
    void scheduleRoundTwoShouldFailWhenInterviewerConflictExists() {
        LocalDateTime start = LocalDateTime.now().plusDays(4).withMinute(0);
        ScheduleRoundTwoInterviewRequest request = new ScheduleRoundTwoInterviewRequest(
                "candidate-5",
                start.plusMinutes(30),
                45,
                "HQ Room 303",
                List.of("interviewer.z"),
                null
        );

        Candidate candidate = new Candidate();
        candidate.setId("candidate-5");
        candidate.setStage(RecruitmentStage.R1_CLEARED);

        Interview existing = new Interview();
        existing.setDateTime(start);
        existing.setDurationMinutes(60);
        existing.setStatus(InterviewStatus.SCHEDULED);
        existing.setInterviewerUsernames(List.of("interviewer.z"));

        when(candidateRepository.findByIdAndDeletedFalse(anyString())).thenReturn(Optional.of(candidate));
        when(interviewRepository.findByStatusInAndDateTimeBetween(anyCollection(), any(), any()))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> interviewService.scheduleRoundTwo(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Scheduling conflict detected for one or more interviewers");

        verify(interviewRepository, never()).save(any(Interview.class));
    }

    @Test
    void getScheduleShouldReturnFilteredInterviewsByDateRangeAndInterviewer() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 20, 9, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 20, 18, 0);

        Interview first = new Interview();
        first.setId("int-1");
        first.setCandidateId("candidate-1");
        first.setRound(InterviewRound.ROUND_1);
        first.setDateTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        first.setDurationMinutes(45);
        first.setStatus(InterviewStatus.SCHEDULED);
        first.setInterviewerUsernames(List.of("interviewer.a"));

        Interview second = new Interview();
        second.setId("int-2");
        second.setCandidateId("candidate-2");
        second.setRound(InterviewRound.ROUND_2);
        second.setDateTime(LocalDateTime.of(2026, 5, 20, 15, 0));
        second.setDurationMinutes(60);
        second.setStatus(InterviewStatus.SCHEDULED);
        second.setInterviewerUsernames(List.of("interviewer.a", "interviewer.b"));

        when(interviewRepository.findSchedule(
                InterviewStatus.SCHEDULED,
                from,
                to,
                "interviewer.a"
        )).thenReturn(List.of(first, second));

        when(candidateRepository.findByIdInAndDeletedFalse(anyCollection())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Collection<String> ids = (Collection<String>) invocation.getArgument(0);
            return ids.stream().map(id -> {
                Candidate c = new Candidate();
                c.setId(id);
                c.setName(id.equals("candidate-1") ? "First Candidate" : "Second Candidate");
                return c;
            }).toList();
        });

        List<InterviewResponse> result = interviewService.getSchedule(from, to, " Interviewer.A ");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("int-1");
        assertThat(result.get(0).candidateName()).isEqualTo("First Candidate");
        assertThat(result.get(1).id()).isEqualTo("int-2");
        assertThat(result.get(1).candidateName()).isEqualTo("Second Candidate");
        verify(interviewRepository).findSchedule(InterviewStatus.SCHEDULED, from, to, "interviewer.a");
        verify(candidateRepository).findByIdInAndDeletedFalse(anyCollection());
    }

    @Test
    void getScheduleShouldTreatBlankInterviewerAsUnfiltered() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 22, 8, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 22, 20, 0);

        when(interviewRepository.findSchedule(InterviewStatus.SCHEDULED, from, to, null)).thenReturn(List.of());

        interviewService.getSchedule(from, to, "   ");

        verify(interviewRepository).findSchedule(InterviewStatus.SCHEDULED, from, to, null);
    }

    @Test
    void getScheduleShouldFailWhenDateRangeIsInvalid() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 21, 12, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 21, 11, 0);

        assertThatThrownBy(() -> interviewService.getSchedule(from, to, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("toDateTime must be on or after fromDateTime");
    }

    @Test
    void rescheduleInterviewShouldUpdateInterviewAndPublishRescheduledEvent() {
        LocalDateTime oldStart = LocalDateTime.now().plusDays(1).withMinute(0);
        LocalDateTime newStart = oldStart.plusDays(1);

        Interview existing = new Interview();
        existing.setId("interview-10");
        existing.setCandidateId("candidate-10");
        existing.setRound(InterviewRound.ROUND_1);
        existing.setStatus(InterviewStatus.SCHEDULED);
        existing.setDateTime(oldStart);
        existing.setDurationMinutes(45);
        existing.setMeetingLink("https://meet.google.com/old");
        existing.setInterviewerUsernames(List.of("interviewer.a"));
        existing.setNotes("Existing notes");

        RescheduleInterviewRequest request = new RescheduleInterviewRequest(
                newStart,
                60,
                List.of("interviewer.a", "interviewer.b"),
                "https://meet.google.com/new",
                null,
                "Updated agenda",
                "Panel availability changed"
        );

        when(interviewRepository.findById("interview-10")).thenReturn(Optional.of(existing));
        when(interviewRepository.findByStatusInAndDateTimeBetween(anyCollection(), any(), any()))
                .thenReturn(List.of(existing));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("recruiter.user", null, List.of())
        );

        InterviewResponse response = interviewService.rescheduleInterview("interview-10", request);

        assertThat(response.id()).isEqualTo("interview-10");
        assertThat(response.durationMinutes()).isEqualTo(60);
        assertThat(response.dateTime()).isEqualTo(newStart);
        assertThat(response.interviewerUsernames()).containsExactly("interviewer.a", "interviewer.b");
        assertThat(existing.getNotes()).contains("Updated agenda");
        assertThat(existing.getNotes()).contains("rescheduled at");
        ArgumentCaptor<InterviewHistory> historyCaptor = ArgumentCaptor.forClass(InterviewHistory.class);
        verify(interviewHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getActionType()).isEqualTo(InterviewActionType.RESCHEDULED);

        ArgumentCaptor<InterviewRescheduledEvent> eventCaptor = ArgumentCaptor.forClass(InterviewRescheduledEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().interviewId()).isEqualTo("interview-10");
        assertThat(eventCaptor.getValue().previousDateTime()).isEqualTo(oldStart);
        assertThat(eventCaptor.getValue().newDateTime()).isEqualTo(newStart);
    }

    @Test
    void rescheduleInterviewShouldFailWhenInterviewerConflictExists() {
        LocalDateTime oldStart = LocalDateTime.now().plusDays(1).withMinute(0);
        LocalDateTime newStart = oldStart.plusDays(1);

        Interview existing = new Interview();
        existing.setId("interview-11");
        existing.setCandidateId("candidate-11");
        existing.setRound(InterviewRound.ROUND_2);
        existing.setStatus(InterviewStatus.SCHEDULED);
        existing.setDateTime(oldStart);
        existing.setDurationMinutes(45);
        existing.setLocation("Room A");
        existing.setInterviewerUsernames(List.of("interviewer.x"));

        Interview conflicting = new Interview();
        conflicting.setId("interview-99");
        conflicting.setStatus(InterviewStatus.SCHEDULED);
        conflicting.setDateTime(newStart.plusMinutes(15));
        conflicting.setDurationMinutes(60);
        conflicting.setInterviewerUsernames(List.of("interviewer.x"));

        RescheduleInterviewRequest request = new RescheduleInterviewRequest(
                newStart,
                45,
                List.of("interviewer.x"),
                null,
                "Room B",
                null,
                null
        );

        when(interviewRepository.findById("interview-11")).thenReturn(Optional.of(existing));
        when(interviewRepository.findByStatusInAndDateTimeBetween(anyCollection(), any(), any()))
                .thenReturn(List.of(existing, conflicting));

        assertThatThrownBy(() -> interviewService.rescheduleInterview("interview-11", request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Scheduling conflict detected for one or more interviewers");
    }

    @Test
    void cancelInterviewShouldCancelAndRollbackStageForRoundOne() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(2);
        Interview interview = new Interview();
        interview.setId("interview-c1");
        interview.setCandidateId("candidate-c1");
        interview.setRound(InterviewRound.ROUND_1);
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setDateTime(scheduledAt);
        interview.setDurationMinutes(45);
        interview.setMeetingLink("https://meet.google.com/c1-test");
        interview.setInterviewerUsernames(List.of("interviewer.a"));
        interview.setNotes("Initial notes");

        Candidate candidate = new Candidate();
        candidate.setId("candidate-c1");
        candidate.setName("Test Candidate");
        candidate.setEmail("test@example.com");
        candidate.setStage(RecruitmentStage.R1_SCHEDULED);

        when(interviewRepository.findById("interview-c1")).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(candidateRepository.findByIdAndDeletedFalse("candidate-c1")).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("recruiter.user", null, List.of())
        );

        InterviewResponse response = interviewService.cancelInterview("interview-c1", new CancelInterviewRequest("No panel"));

        assertThat(response.status()).isEqualTo(InterviewStatus.CANCELLED);
        assertThat(candidate.getStage()).isEqualTo(RecruitmentStage.SHORTLISTED);
        assertThat(interview.getNotes()).contains("Interview cancelled. Reason: No panel");
        verify(stageHistoryRepository).save(any(StageHistory.class));
        ArgumentCaptor<InterviewHistory> historyCaptor = ArgumentCaptor.forClass(InterviewHistory.class);
        verify(interviewHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getActionType()).isEqualTo(InterviewActionType.CANCELLED);

        ArgumentCaptor<InterviewCancelledEvent> eventCaptor = ArgumentCaptor.forClass(InterviewCancelledEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().interviewId()).isEqualTo("interview-c1");
        assertThat(eventCaptor.getValue().candidateName()).isEqualTo("Test Candidate");
        assertThat(eventCaptor.getValue().reason()).isEqualTo("No panel");
    }

    @Test
    void cancelInterviewShouldFailWhenAlreadyCancelled() {
        Interview interview = new Interview();
        interview.setId("interview-c2");
        interview.setStatus(InterviewStatus.CANCELLED);

        when(interviewRepository.findById("interview-c2")).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> interviewService.cancelInterview("interview-c2", new CancelInterviewRequest("Duplicate")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Interview is already cancelled");
    }

    @Test
    void cancelInterviewShouldRollbackToR1ClearedForRoundTwo() {
        Interview interview = new Interview();
        interview.setId("interview-c3");
        interview.setCandidateId("candidate-c3");
        interview.setRound(InterviewRound.ROUND_2);
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setDateTime(LocalDateTime.now().plusDays(3));
        interview.setDurationMinutes(60);
        interview.setLocation("Room 5B");
        interview.setInterviewerUsernames(List.of("interviewer.b"));

        Candidate candidate = new Candidate();
        candidate.setId("candidate-c3");
        candidate.setName("R2 Candidate");
        candidate.setEmail("r2@example.com");
        candidate.setStage(RecruitmentStage.R2_SCHEDULED);

        when(interviewRepository.findById("interview-c3")).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(candidateRepository.findByIdAndDeletedFalse("candidate-c3")).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("recruiter.user", null, List.of())
        );

        interviewService.cancelInterview("interview-c3", new CancelInterviewRequest("Panel unavailable"));

        assertThat(candidate.getStage()).isEqualTo(RecruitmentStage.R1_CLEARED);
        verify(applicationEventPublisher).publishEvent(any(InterviewCancelledEvent.class));
    }

    @Test
    void cancelInterviewShouldTrimNotesToEntityLimit() {
        Interview interview = new Interview();
        interview.setId("interview-c5");
        interview.setCandidateId("candidate-c5");
        interview.setRound(InterviewRound.ROUND_1);
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setDateTime(LocalDateTime.now().plusDays(1));
        interview.setDurationMinutes(30);
        interview.setMeetingLink("https://meet.google.com/c5");
        interview.setInterviewerUsernames(List.of("interviewer.c"));
        interview.setNotes("a".repeat(995));

        Candidate candidate = new Candidate();
        candidate.setId("candidate-c5");
        candidate.setName("Trim Candidate");
        candidate.setStage(RecruitmentStage.R1_SCHEDULED);

        when(interviewRepository.findById("interview-c5")).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(candidateRepository.findByIdAndDeletedFalse("candidate-c5")).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("recruiter.user", null, List.of())
        );

        interviewService.cancelInterview("interview-c5", new CancelInterviewRequest("No panel"));

        assertThat(interview.getNotes()).hasSize(1000);
        assertThat(interview.getNotes()).endsWith("Interview cancelled. Reason: No panel");
    }

    @Test
    void cancelInterviewWithNullRequestShouldRecordDefaultCancellationMessage() {
        Interview interview = new Interview();
        interview.setId("interview-c6");
        interview.setCandidateId("candidate-c6");
        interview.setRound(InterviewRound.ROUND_1);
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setDateTime(LocalDateTime.now().plusDays(1));
        interview.setDurationMinutes(45);
        interview.setMeetingLink("https://meet.google.com/c6");
        interview.setInterviewerUsernames(List.of("interviewer.d"));

        Candidate candidate = new Candidate();
        candidate.setId("candidate-c6");
        candidate.setName("Null Request Candidate");
        candidate.setEmail("null-req@example.com");
        candidate.setStage(RecruitmentStage.R1_SCHEDULED);

        when(interviewRepository.findById("interview-c6")).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(candidateRepository.findByIdAndDeletedFalse("candidate-c6")).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("recruiter.user", null, List.of())
        );

        interviewService.cancelInterview("interview-c6", null);

        assertThat(interview.getNotes()).isEqualTo("Interview cancelled");
        ArgumentCaptor<InterviewHistory> historyCaptor = ArgumentCaptor.forClass(InterviewHistory.class);
        verify(interviewHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getDetails()).isEqualTo("Interview cancelled");

        ArgumentCaptor<InterviewCancelledEvent> eventCaptor = ArgumentCaptor.forClass(InterviewCancelledEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().reason()).isNull();
    }

    @Test
    void cancelInterviewShouldFailWhenCompleted() {
        Interview interview = new Interview();
        interview.setId("interview-c4");
        interview.setStatus(InterviewStatus.COMPLETED);

        when(interviewRepository.findById("interview-c4")).thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> interviewService.cancelInterview("interview-c4", new CancelInterviewRequest("Late change")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Completed interviews cannot be cancelled");
    }
}
