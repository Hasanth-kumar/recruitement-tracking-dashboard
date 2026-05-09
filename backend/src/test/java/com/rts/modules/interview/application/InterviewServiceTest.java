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

    private InterviewService interviewService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        interviewService = new InterviewService(
                interviewRepository,
                candidateRepository,
                stageHistoryRepository,
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

        List<InterviewResponse> result = interviewService.getSchedule(from, to, " Interviewer.A ");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("int-1");
        assertThat(result.get(1).id()).isEqualTo("int-2");
        verify(interviewRepository).findSchedule(InterviewStatus.SCHEDULED, from, to, "interviewer.a");
    }

    @Test
    void getScheduleShouldFailWhenDateRangeIsInvalid() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 21, 12, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 21, 11, 0);

        assertThatThrownBy(() -> interviewService.getSchedule(from, to, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("toDateTime must be on or after fromDateTime");
    }
}
