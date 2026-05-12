package com.rts.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailAdapter implements EmailPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailAdapter.class);

    private final JavaMailSender mailSender;
    private final EmailAuditRepository emailAuditRepository;
    private final boolean mailEnabled;
    private final String fromAddress;

    public SmtpEmailAdapter(
            JavaMailSender mailSender,
            EmailAuditRepository emailAuditRepository,
            @Value("${rts.mail.enabled:true}") boolean mailEnabled,
            @Value("${rts.mail.from:noreply@rts.com}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.emailAuditRepository = emailAuditRepository;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
    }

    @Override
    @Async
    public void send(String to, String subject, String body) {
        if (!mailEnabled) {
            log.debug("Mail disabled — skipping email to {} with subject '{}'", to, subject);
            saveAudit(to, subject, EmailAudit.EmailStatus.SKIPPED, null);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} — subject: {}", to, subject);
            saveAudit(to, subject, EmailAudit.EmailStatus.SENT, null);
        } catch (MailException ex) {
            log.error("Failed to send email to {} — subject: '{}': {}", to, subject, ex.getMessage());
            saveAudit(to, subject, EmailAudit.EmailStatus.FAILED, ex.getMessage());
        }
    }

    private void saveAudit(String recipient, String subject, EmailAudit.EmailStatus status, String errorMessage) {
        try {
            EmailAudit audit = new EmailAudit();
            audit.setRecipient(recipient);
            audit.setSubject(subject);
            audit.setStatus(status);
            if (errorMessage != null && errorMessage.length() > 1000) {
                errorMessage = errorMessage.substring(0, 1000);
            }
            audit.setErrorMessage(errorMessage);
            emailAuditRepository.save(audit);
        } catch (Exception ex) {
            log.error("Failed to save email audit for recipient '{}': {}", recipient, ex.getMessage());
        }
    }
}
