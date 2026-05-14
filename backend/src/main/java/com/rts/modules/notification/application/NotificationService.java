package com.rts.modules.notification.application;

import com.rts.infrastructure.mail.EmailPort;
import com.rts.infrastructure.notification.UserEmailResolverPort;
import com.rts.modules.feedback.persistence.FeedbackRepository;
import com.rts.modules.interview.domain.Interview;
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
import java.util.Map;
import java.util.Set;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /** In-app copy; must not exceed {@code Notification} message column (500). */
    private static final int NOTIFICATION_MESSAGE_MAX_LEN = 500;
    private static final int FEEDBACK_OVERDUE_HOURS = 24;
    private static final int LOOKBACK_DAYS = 7;

    private final NotificationRepository notificationRepository;
    private final InterviewRepository interviewRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserEmailResolverPort userEmailResolver;
    private final EmailPort emailPort;

    public NotificationService(
            NotificationRepository notificationRepository,
            InterviewRepository interviewRepository,
            FeedbackRepository feedbackRepository,
            UserEmailResolverPort userEmailResolver,
            EmailPort emailPort
    ) {
        this.notificationRepository = notificationRepository;
        this.interviewRepository = interviewRepository;
        this.feedbackRepository = feedbackRepository;
        this.userEmailResolver = userEmailResolver;
        this.emailPort = emailPort;
    }

    private static String clipNotificationMessage(String message) {
        if (message == null) {
            return "";
        }
        if (message.length() <= NOTIFICATION_MESSAGE_MAX_LEN) {
            return message;
        }
        return message.substring(0, NOTIFICATION_MESSAGE_MAX_LEN - 3) + "...";
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
                notification.setMessage(clipNotificationMessage(
                        "Feedback pending: Your %s interview for candidate %s on %s is awaiting feedback submission."
                                .formatted(
                                        interview.getRound().name().replace('_', ' '),
                                        interview.getCandidateId(),
                                        interview.getDateTime().format(DATETIME_FORMAT)
                                )
                ));
                notifications.add(notification);
            }
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
            log.info("Created {} pending-feedback reminder notifications for {} overdue interviews",
                    notifications.size(), overdueInterviews.size());
            sendEmailsForNotifications(notifications);
        }
    }

    @EventListener
    @Transactional
    public void handleInterviewScheduled(InterviewScheduledEvent event) {
        List<Notification> notifications = new ArrayList<>();
        String scheduledFor = event.scheduledAt().format(DATETIME_FORMAT);
        String roundLabel = event.round().name().replace('_', ' ');

        for (String interviewer : event.interviewerUsernames()) {
            Notification notification = new Notification();
            notification.setUserId(interviewer);
            notification.setType(NotificationType.INTERVIEW_SCHEDULED);
            notification.setRead(false);
            notification.setMessage(clipNotificationMessage(
                    "%s interview scheduled for candidate %s at %s (%d mins)."
                            .formatted(roundLabel, event.candidateName(), scheduledFor, event.durationMinutes())
            ));
            notifications.add(notification);
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
            sendEmailsForNotifications(notifications);
        }

        sendScheduledInterviewerEmails(event);
        sendScheduledCandidateEmail(event);
    }

    @EventListener
    @Transactional
    public void handleInterviewRescheduled(InterviewRescheduledEvent event) {
        List<Notification> notifications = new ArrayList<>();
        String previousTime = event.previousDateTime().format(DATETIME_FORMAT);
        String newTime = event.newDateTime().format(DATETIME_FORMAT);
        String roundLabel = event.round().name().replace('_', ' ');
        String reasonSuffix = event.reason() == null || event.reason().isBlank()
                ? ""
                : " Reason: " + event.reason().trim();

        for (String interviewer : event.interviewerUsernames()) {
            Notification notification = new Notification();
            notification.setUserId(interviewer);
            notification.setType(NotificationType.INTERVIEW_RESCHEDULED);
            notification.setRead(false);
            notification.setMessage(clipNotificationMessage(
                    "%s interview for candidate %s was rescheduled from %s to %s (%d mins).%s"
                            .formatted(roundLabel, event.candidateName(), previousTime, newTime,
                                    event.durationMinutes(), reasonSuffix)
            ));
            notifications.add(notification);
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
            sendEmailsForNotifications(notifications);
        }

        sendRescheduledInterviewerEmails(event);
        sendRescheduledCandidateEmail(event);
    }

    private void sendEmailsForNotifications(List<Notification> notifications) {
        List<String> usernames = notifications.stream()
                .map(Notification::getUserId)
                .distinct()
                .toList();

        Map<String, String> usernameToEmail = userEmailResolver.resolveEmails(usernames);

        for (Notification notification : notifications) {
            String email = usernameToEmail.get(notification.getUserId());
            if (email == null) {
                log.warn("No email found for user '{}' — skipping email notification", notification.getUserId());
                continue;
            }
            String subject = resolveEmailSubject(notification.getType());
            emailPort.send(email, subject, notification.getMessage());
        }
    }

    private String resolveEmailSubject(NotificationType type) {
        return switch (type) {
            case INTERVIEW_SCHEDULED -> "RTS — Interview Scheduled";
            case INTERVIEW_RESCHEDULED -> "RTS — Interview Rescheduled";
            case INTERVIEW_CANCELLED -> "RTS — Interview Cancelled";
            case FEEDBACK_PENDING -> "RTS — Feedback Reminder";
            case STAGE_UPDATED -> "RTS — Candidate Stage Updated";
            case CANDIDATE_REGISTERED -> "RTS — Candidate Registered";
            case PASSWORD_RESET -> "RTS — Password Reset";
            case EMAIL_VERIFICATION -> "RTS — Email Verification";
        };
    }

    private void sendScheduledInterviewerEmails(InterviewScheduledEvent event) {
        String roundLabel = event.round().name().replace('_', ' ');
        String scheduledFor = event.scheduledAt().format(DATETIME_FORMAT);

        Map<String, String> usernameToEmail = userEmailResolver.resolveEmails(event.interviewerUsernames());

        for (String interviewer : event.interviewerUsernames()) {
            String email = usernameToEmail.get(interviewer);
            if (email == null) {
                log.warn("No email found for interviewer '{}' — skipping schedule email", interviewer);
                continue;
            }

            String body = buildScheduledEmailBody(
                    roundLabel, event.candidateName(), scheduledFor,
                    event.durationMinutes(), event.interviewerUsernames(),
                    event.meetingLink(), event.location(), event.notes()
            );
            emailPort.send(email, "RTS — %s Interview Scheduled".formatted(roundLabel), body);
        }
    }

    private void sendScheduledCandidateEmail(InterviewScheduledEvent event) {
        if (event.candidateEmail() == null || event.candidateEmail().isBlank()) {
            log.warn("No email for candidate '{}' — skipping schedule email", event.candidateId());
            return;
        }

        String roundLabel = event.round().name().replace('_', ' ');
        String scheduledFor = event.scheduledAt().format(DATETIME_FORMAT);

        String body = buildScheduledEmailBody(
                roundLabel, event.candidateName(), scheduledFor,
                event.durationMinutes(), event.interviewerUsernames(),
                event.meetingLink(), event.location(), event.notes()
        );
        emailPort.send(event.candidateEmail(),
                "RTS — Your %s Interview Has Been Scheduled".formatted(roundLabel), body);
    }

    private String buildScheduledEmailBody(
            String roundLabel, String candidateName, String scheduledFor,
            int durationMinutes, List<String> interviewers,
            String meetingLink, String location, String notes
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Interview Details\n");
        sb.append("=================\n\n");
        sb.append("Round:         ").append(roundLabel).append("\n");
        sb.append("Candidate:     ").append(candidateName).append("\n");
        sb.append("Date/Time:     ").append(scheduledFor).append("\n");
        sb.append("Duration:      ").append(durationMinutes).append(" minutes\n");
        sb.append("Interviewers:  ").append(String.join(", ", interviewers)).append("\n");
        if (meetingLink != null && !meetingLink.isBlank()) {
            sb.append("Meeting Link:  ").append(meetingLink).append("\n");
        }
        if (location != null && !location.isBlank()) {
            sb.append("Location:      ").append(location).append("\n");
        }
        if (notes != null && !notes.isBlank()) {
            sb.append("\nNotes:\n").append(notes).append("\n");
        }
        sb.append("\n— Recruitment Tracking System");
        return sb.toString();
    }

    private void sendRescheduledInterviewerEmails(InterviewRescheduledEvent event) {
        String roundLabel = event.round().name().replace('_', ' ');
        String previousTime = event.previousDateTime().format(DATETIME_FORMAT);
        String newTime = event.newDateTime().format(DATETIME_FORMAT);

        Map<String, String> usernameToEmail = userEmailResolver.resolveEmails(event.interviewerUsernames());

        for (String interviewer : event.interviewerUsernames()) {
            String email = usernameToEmail.get(interviewer);
            if (email == null) {
                log.warn("No email found for interviewer '{}' — skipping reschedule email", interviewer);
                continue;
            }

            String body = buildRescheduledEmailBody(
                    roundLabel, event.candidateName(), previousTime, newTime,
                    event.durationMinutes(), event.interviewerUsernames(),
                    event.meetingLink(), event.location(), event.notes(), event.reason()
            );
            emailPort.send(email, "RTS — %s Interview Rescheduled".formatted(roundLabel), body);
        }
    }

    private void sendRescheduledCandidateEmail(InterviewRescheduledEvent event) {
        if (event.candidateEmail() == null || event.candidateEmail().isBlank()) {
            log.warn("No email for candidate '{}' — skipping reschedule email", event.candidateId());
            return;
        }

        String roundLabel = event.round().name().replace('_', ' ');
        String previousTime = event.previousDateTime().format(DATETIME_FORMAT);
        String newTime = event.newDateTime().format(DATETIME_FORMAT);

        String body = buildRescheduledEmailBody(
                roundLabel, event.candidateName(), previousTime, newTime,
                event.durationMinutes(), event.interviewerUsernames(),
                event.meetingLink(), event.location(), event.notes(), event.reason()
        );
        emailPort.send(event.candidateEmail(),
                "RTS — Your %s Interview Has Been Rescheduled".formatted(roundLabel), body);
    }

    private String buildRescheduledEmailBody(
            String roundLabel, String candidateName, String previousTime, String newTime,
            int durationMinutes, List<String> interviewers,
            String meetingLink, String location, String notes, String reason
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Interview Rescheduled\n");
        sb.append("=====================\n\n");
        sb.append("Round:             ").append(roundLabel).append("\n");
        sb.append("Candidate:         ").append(candidateName).append("\n");
        sb.append("Previous Time:     ").append(previousTime).append("\n");
        sb.append("New Date/Time:     ").append(newTime).append("\n");
        sb.append("Duration:          ").append(durationMinutes).append(" minutes\n");
        sb.append("Interviewers:      ").append(String.join(", ", interviewers)).append("\n");
        if (meetingLink != null && !meetingLink.isBlank()) {
            sb.append("Meeting Link:      ").append(meetingLink).append("\n");
        }
        if (location != null && !location.isBlank()) {
            sb.append("Location:          ").append(location).append("\n");
        }
        if (reason != null && !reason.isBlank()) {
            sb.append("Reason:            ").append(reason).append("\n");
        }
        if (notes != null && !notes.isBlank()) {
            sb.append("\nNotes:\n").append(notes).append("\n");
        }
        sb.append("\n— Recruitment Tracking System");
        return sb.toString();
    }

    @EventListener
    @Transactional
    public void handleInterviewCancelled(InterviewCancelledEvent event) {
        List<Notification> notifications = new ArrayList<>();
        String scheduledFor = event.scheduledAt().format(DATETIME_FORMAT);
        String roundLabel = event.round().name().replace('_', ' ');
        String reasonSuffix = event.reason() == null || event.reason().isBlank()
                ? ""
                : " Reason: " + event.reason().trim();

        for (String interviewer : event.interviewerUsernames()) {
            Notification notification = new Notification();
            notification.setUserId(interviewer);
            notification.setType(NotificationType.INTERVIEW_CANCELLED);
            notification.setRead(false);
            notification.setMessage(clipNotificationMessage(
                    "%s interview for candidate %s scheduled at %s has been cancelled.%s"
                            .formatted(roundLabel, event.candidateName(), scheduledFor, reasonSuffix)
            ));
            notifications.add(notification);
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
            sendEmailsForNotifications(notifications);
        }

        sendCancelledInterviewerEmails(event);
        sendCancelledCandidateEmail(event);
    }

    private void sendCancelledInterviewerEmails(InterviewCancelledEvent event) {
        String roundLabel = event.round().name().replace('_', ' ');
        String scheduledFor = event.scheduledAt().format(DATETIME_FORMAT);

        Map<String, String> usernameToEmail = userEmailResolver.resolveEmails(event.interviewerUsernames());

        for (String interviewer : event.interviewerUsernames()) {
            String email = usernameToEmail.get(interviewer);
            if (email == null) {
                log.warn("No email found for interviewer '{}' — skipping cancellation email", interviewer);
                continue;
            }

            String body = buildCancelledEmailBody(
                    roundLabel, event.candidateName(), scheduledFor,
                    event.durationMinutes(), event.interviewerUsernames(),
                    event.meetingLink(), event.location(), event.reason()
            );
            emailPort.send(email, "RTS — %s Interview Cancelled".formatted(roundLabel), body);
        }
    }

    private void sendCancelledCandidateEmail(InterviewCancelledEvent event) {
        if (event.candidateEmail() == null || event.candidateEmail().isBlank()) {
            log.warn("No email for candidate '{}' — skipping cancellation email", event.candidateId());
            return;
        }

        String roundLabel = event.round().name().replace('_', ' ');
        String scheduledFor = event.scheduledAt().format(DATETIME_FORMAT);

        String body = buildCancelledEmailBody(
                roundLabel, event.candidateName(), scheduledFor,
                event.durationMinutes(), event.interviewerUsernames(),
                event.meetingLink(), event.location(), event.reason()
        );
        emailPort.send(event.candidateEmail(),
                "RTS — Your %s Interview Has Been Cancelled".formatted(roundLabel), body);
    }

    private String buildCancelledEmailBody(
            String roundLabel, String candidateName, String scheduledFor,
            int durationMinutes, List<String> interviewers,
            String meetingLink, String location, String reason
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Interview Cancelled\n");
        sb.append("===================\n\n");
        sb.append("Round:         ").append(roundLabel).append("\n");
        sb.append("Candidate:     ").append(candidateName).append("\n");
        sb.append("Date/Time:     ").append(scheduledFor).append("\n");
        sb.append("Duration:      ").append(durationMinutes).append(" minutes\n");
        sb.append("Interviewers:  ").append(String.join(", ", interviewers)).append("\n");
        if (meetingLink != null && !meetingLink.isBlank()) {
            sb.append("Meeting Link:  ").append(meetingLink).append("\n");
        }
        if (location != null && !location.isBlank()) {
            sb.append("Location:      ").append(location).append("\n");
        }
        if (reason != null && !reason.isBlank()) {
            sb.append("\nReason:\n").append(reason).append("\n");
        }
        sb.append("\nIf this interview needs to be rescheduled, your recruiter will reach out with new details.");
        sb.append("\n\n— Recruitment Tracking System");
        return sb.toString();
    }

    // ── Candidate Registration Confirmation ─────────────────────────────

    @EventListener
    public void handleCandidateRegistered(CandidateRegisteredEvent event) {
        if (event.candidateEmail() == null || event.candidateEmail().isBlank()) {
            log.warn("No email for candidate '{}' — skipping registration confirmation", event.candidateId());
            return;
        }

        String body = buildRegistrationConfirmationBody(event.candidateName(), event.position());
        emailPort.send(event.candidateEmail(),
                "RTS — Application Received for " + event.position(), body);

        log.info("Registration confirmation email sent to candidate '{}'", event.candidateName());
    }

    private String buildRegistrationConfirmationBody(String candidateName, String position) {
        return """
                Application Received
                ====================

                Dear %s,

                Thank you for your interest in the %s position. We have received your application and it is currently under review.

                Our recruitment team will assess your profile and get back to you with the next steps. You may receive updates via email as your application progresses through our review process.

                If you have any questions, please don't hesitate to reach out to our recruitment team.

                Best regards,
                — Recruitment Tracking System""".formatted(candidateName, position);
    }

    // ── Candidate Stage Change Notifications ────────────────────────────

    @EventListener
    public void handleCandidateStageChanged(CandidateStageChangedEvent event) {
        if (event.candidateEmail() == null || event.candidateEmail().isBlank()) {
            log.warn("No email for candidate '{}' — skipping stage update notification", event.candidateId());
            return;
        }

        String subject = resolveStageEmailSubject(event.newStage(), event.position());
        String body = buildStageChangeEmailBody(
                event.candidateName(), event.position(),
                event.previousStage(), event.newStage()
        );
        emailPort.send(event.candidateEmail(), subject, body);

        log.info("Stage update notification sent to candidate '{}': {} -> {}",
                event.candidateName(), event.previousStage(), event.newStage());
    }

    private String resolveStageEmailSubject(RecruitmentStage stage, String position) {
        return switch (stage) {
            case SHORTLISTED -> "RTS — You Have Been Shortlisted for " + position;
            case R1_SCHEDULED -> "RTS — Round 1 Scheduled for " + position;
            case R1_CLEARED -> "RTS — Congratulations! You Cleared Round 1 for " + position;
            case R2_SCHEDULED -> "RTS — Round 2 Scheduled for " + position;
            case R2_CLEARED -> "RTS — Congratulations! You Cleared Round 2 for " + position;
            case OFFERED -> "RTS — Offer Extended for " + position;
            case HIRED -> "RTS — Welcome Aboard! " + position;
            case REJECTED -> "RTS — Application Update for " + position;
            default -> "RTS — Application Status Update for " + position;
        };
    }

    private String buildStageChangeEmailBody(
            String candidateName, String position,
            RecruitmentStage previousStage, RecruitmentStage newStage
    ) {
        String stageMessage = resolveStageMessage(candidateName, position, newStage);
        return """
                Application Status Update
                =========================

                Dear %s,

                %s

                Position: %s
                Previous Status: %s
                Current Status: %s

                If you have any questions, please reach out to our recruitment team.

                Best regards,
                — Recruitment Tracking System""".formatted(
                candidateName,
                stageMessage,
                position,
                formatStage(previousStage),
                formatStage(newStage)
        );
    }

    private String resolveStageMessage(String candidateName, String position, RecruitmentStage stage) {
        return switch (stage) {
            case SCREENING -> "Your application for " + position + " is currently being reviewed by our team.";
            case SHORTLISTED -> "Great news! You have been shortlisted for the " + position + " position. Our team will be in touch with next steps.";
            case R1_SCHEDULED -> "Round 1 of your interview process for " + position + " has been scheduled. You will receive detailed interview information separately.";
            case R1_CLEARED -> "Congratulations! You have successfully cleared Round 1 for the " + position + " position.";
            case R2_SCHEDULED -> "Round 2 of your interview process for " + position + " has been scheduled. You will receive detailed interview information separately.";
            case R2_CLEARED -> "Congratulations! You have successfully cleared Round 2 for the " + position + " position.";
            case OFFERED -> "We are pleased to inform you that an offer has been extended for the " + position + " position. Our HR team will share the detailed offer letter with you.";
            case HIRED -> "Welcome aboard! We are thrilled to have you join our team for the " + position + " position. You will receive onboarding details shortly.";
            case REJECTED -> "Thank you for your interest in the " + position + " position. After careful consideration, we have decided to move forward with other candidates. We encourage you to apply for future opportunities.";
            default -> "Your application status for " + position + " has been updated.";
        };
    }

    private String formatStage(RecruitmentStage stage) {
        return stage.name().replace('_', ' ');
    }

}
