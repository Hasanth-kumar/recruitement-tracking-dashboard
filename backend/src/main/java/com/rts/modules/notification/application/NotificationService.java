package com.rts.modules.notification.application;

import com.rts.modules.notification.domain.Notification;
import com.rts.modules.notification.domain.NotificationType;
import com.rts.modules.notification.persistence.NotificationRepository;
import com.rts.shared.events.InterviewScheduledEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
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
                    "Round 1 interview scheduled for candidate %s at %s (%d mins)."
                            .formatted(event.candidateId(), scheduledFor, event.durationMinutes())
            );
            notifications.add(notification);
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }
}
