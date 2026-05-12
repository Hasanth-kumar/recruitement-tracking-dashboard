package com.rts.modules.auth.application;

import com.rts.infrastructure.mail.EmailPort;
import com.rts.modules.auth.domain.EmailVerificationToken;
import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.EmailVerificationTokenRepository;
import com.rts.modules.auth.persistence.UserRepository;
import com.rts.shared.exception.ConflictException;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailPort emailPort;
    private final String baseUrl;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailPort emailPort,
            @Value("${rts.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailPort = emailPort;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public void initiateEmailChange(User user, String newEmail) {
        String normalizedEmail = newEmail.trim().toLowerCase(Locale.ROOT);

        if (normalizedEmail.equals(user.getEmail())) {
            throw new ValidationException("New email is the same as current email");
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("Email already in use by another account");
        }

        tokenRepository.deleteUnconfirmedByUserId(user.getId());

        String rawToken = generateSecureToken();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setToken(rawToken);
        token.setNewEmail(normalizedEmail);
        tokenRepository.save(token);

        user.setPendingEmail(normalizedEmail);
        userRepository.save(user);

        String verificationLink = baseUrl + "/api/auth/verify-email?token=" + rawToken;
        String body = buildVerificationEmailBody(user.getUsername(), normalizedEmail, verificationLink);
        emailPort.send(normalizedEmail, "RTS — Verify Your New Email Address", body);

        log.info("Email verification token sent to {} for user '{}'", normalizedEmail, user.getUsername());
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenAndConfirmedFalse(rawToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or already used verification token"));

        if (token.isExpired()) {
            throw new ValidationException("Verification token has expired. Please request a new email change.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userRepository.findByEmail(token.getNewEmail()).isPresent()) {
            throw new ConflictException("Email is already in use by another account");
        }

        user.setEmail(token.getNewEmail());
        user.setEmailVerified(true);
        user.setPendingEmail(null);
        userRepository.save(user);

        token.setConfirmed(true);
        tokenRepository.save(token);

        log.info("Email verified and updated for user '{}' to '{}'", user.getUsername(), token.getNewEmail());
    }

    @Transactional
    public void resendVerification(User user) {
        if (user.getPendingEmail() == null || user.getPendingEmail().isBlank()) {
            throw new ValidationException("No pending email change to verify");
        }

        tokenRepository.deleteUnconfirmedByUserId(user.getId());

        String rawToken = generateSecureToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId(user.getId());
        token.setToken(rawToken);
        token.setNewEmail(user.getPendingEmail());
        tokenRepository.save(token);

        String verificationLink = baseUrl + "/api/auth/verify-email?token=" + rawToken;
        String body = buildVerificationEmailBody(user.getUsername(), user.getPendingEmail(), verificationLink);
        emailPort.send(user.getPendingEmail(), "RTS — Verify Your New Email Address", body);

        log.info("Resent email verification to {} for user '{}'", user.getPendingEmail(), user.getUsername());
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredBefore(LocalDateTime.now());
        log.debug("Cleaned up expired email verification tokens");
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildVerificationEmailBody(String username, String newEmail, String verificationLink) {
        return """
                Email Verification
                ==================

                Hi %s,

                You requested to change your email address to: %s

                Please verify your new email by clicking the link below:
                %s

                This link will expire in 24 hours. If you did not request this change, please ignore this email.

                — Recruitment Tracking System""".formatted(username, newEmail, verificationLink);
    }
}
