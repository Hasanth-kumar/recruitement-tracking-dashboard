package com.rts.modules.notification.application;

import com.rts.modules.feedback.persistence.FeedbackRepository;
import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewStatus;
import com.rts.modules.interview.persistence.InterviewRepository;
import com.rts.modules.notification.domain.Notification;
import com.rts.modules.notification.domain.NotificationType;
import com.rts.modules.notification.persistence.NotificationRepository;
import com.rts.shared.events.InterviewRescheduledEvent;
import com.rts.shared.events.InterviewScheduledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int FEEDBACK_OVERDUE_HOURS = 24;
    private static final int LOOKBACK_DAYS = 7;

    private final NotificationRepository notificationRepository;
    private final InterviewRepository interviewRepository;
    private final FeedbackRepository feedbackRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            InterviewRepository interviewRepository,
            FeedbackRepository feedbackRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.interviewRepository = interviewRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void checkPendingFeedback() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(FEEDBACK_OVERDUE_HOURS);
        LocalDateTime earliest = now.minusDays(LOOKBACK_DAYS);

        List<Interview> interviews = interviewRepository.findInterviewsScheduledBetween(
                Set.of(InterviewStatus.SCHEDULED, InterviewStatus.COMPLETED),
                cutoff,
                earliest
        );

        if (interviews.isEmpty()) {
            return;
        }

        List<String> interviewIds = interviews.stream().map(Interview::getId).toList();
        List<String> interviewIdsWithFeedback = feedbackRepository.findInterviewIdsWithFeedback(interviewIds);
        Set<String> withFeedbackSet = Set.copyOf(interviewIdsWithFeedback);

        List<Interview> overdueInterviews = interviews.stream()
                .filter(i -> !withFeedbackSet.contains(i.getId()))
                .filter(i -> {
                    LocalDateTime endTime = i.getDateTime().plusMinutes(i.getDurationMinutes());
                    return endTime.plusHours(FEEDBACK_OVERDUE_HOURS).isBefore(now);
                })
                .toList();

        if (overdueInterviews.isEmpty()) {
            return;
        }

        List<Notification> notifications = new ArrayList<>();
        for (Interview interview : overdueInterviews) {
            for (String interviewer : interview.getInterviewerUsernames()) {
                Notification notification = new Notification();
                notification.setUserId(interviewer);
                notification.setType(NotificationType.FEEDBACK_PENDING);
                notification.setRead(false);
                notification.setMessage(
                        "Feedback pending: Your %s interview for candidate %s on %s is awaiting feedback submission."
                                .formatted(
                                        interview.getRound().name().replace('_', ' '),
                                        interview.getCandidateId(),
                                        interview.getDateTime().format(DATETIME_FORMAT)
                                )
                );
                notifications.add(notification);
            }
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
            log.info("Created {} pending-feedback reminder notifications for {} overdue interviews",
                    notifications.size(), overdueInterviews.size());
        }
    }

    @EventListener
    @Transactional
    public void handleInterviewScheduled(InterviewScheduledEvent event) {
        List<Notification> notifications = new ArrayList<>();
        String scheduledFor = event.scheduledAt().format(DATETIME_FORMAT);

        for (String interviewer : event.interviewerUsernames()) {
            Notification notification = new Notification();
            notification.setUserId(interviewer);
            notification.setType(NotificationType.INTERVIEW_SCHEDULED);
            notification.setRead(false);
            notification.setMessage(
                    "%s interview scheduled for candidate %s at %s (%d mins)."
                            .formatted(event.round().name().replace('_', ' '), event.candidateId(), scheduledFor, event.durationMinutes())
            );
            notifications.add(notification);
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    @EventListener
    @Transactional
    public void handleInterviewRescheduled(InterviewRescheduledEvent event) {
        List<Notification> notifications = new ArrayList<>();
        String previousTime = event.previousDateTime().format(DATETIME_FORMAT);
        String newTime = event.newDateTime().format(DATETIME_FORMAT);
        String reasonSuffix = event.reason() == null || event.reason().isBlank()
                ? ""
                : " Reason: " + event.reason().trim();

        for (String interviewer : event.interviewerUsernames()) {
            Notification notification = new Notification();
            notification.setUserId(interviewer);
            notification.setType(NotificationType.INTERVIEW_RESCHEDULED);
            notification.setRead(false);
            notification.setMessage(
                    "%s interview for candidate %s was rescheduled from %s to %s (%d mins).%s"
                            .formatted(
                                    event.round().name().replace('_', ' '),
                                    event.candidateId(),
                                    previousTime,
                                    newTime,
                                    event.durationMinutes(),
                                    reasonSuffix
                            )
            );
            notifications.add(notification);
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }
}
