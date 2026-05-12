package com.rts.modules.auth.application;

import com.rts.infrastructure.mail.EmailPort;
import com.rts.modules.auth.domain.PasswordResetToken;
import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.PasswordResetTokenRepository;
import com.rts.modules.auth.persistence.UserRepository;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailPort emailPort;
    private final String baseUrl;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailPort emailPort,
            @Value("${rts.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailPort = emailPort;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email.trim().toLowerCase());
        if (optionalUser.isEmpty()) {
            log.debug("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = optionalUser.get();
        if (user.isDeleted()) {
            log.debug("Password reset requested for deleted user: {}", email);
            return;
        }

        tokenRepository.deleteUnusedByUserId(user.getId());

        String rawToken = generateSecureToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setToken(rawToken);
        tokenRepository.save(resetToken);

        String resetLink = baseUrl + "/reset-password?token=" + rawToken;
        String body = buildResetEmailBody(user.getUsername(), resetLink);
        emailPort.send(user.getEmail(), "RTS — Password Reset Request", body);

        log.info("Password reset token sent to user '{}'", user.getUsername());
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("Password and confirm password do not match");
        }

        PasswordResetToken token = tokenRepository.findByTokenAndUsedFalse(rawToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or already used reset token"));

        if (token.isExpired()) {
            throw new ValidationException("Password reset token has expired. Please request a new one.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        emailPort.send(user.getEmail(), "RTS — Your Password Has Been Reset",
                buildPasswordChangedEmailBody(user.getUsername()));

        log.info("Password successfully reset for user '{}'", user.getUsername());
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredBefore(LocalDateTime.now());
        log.debug("Cleaned up expired password reset tokens");
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildResetEmailBody(String username, String resetLink) {
        return """
                Password Reset Request
                ======================

                Hi %s,

                We received a request to reset your password. Click the link below to set a new password:
                %s

                This link will expire in 30 minutes. If you did not request a password reset, you can safely ignore this email.

                — Recruitment Tracking System""".formatted(username, resetLink);
    }

    private String buildPasswordChangedEmailBody(String username) {
        return """
                Password Changed
                ================

                Hi %s,

                Your password has been successfully changed. If you did not make this change, please contact your administrator immediately.

                — Recruitment Tracking System""".formatted(username);
    }
}
