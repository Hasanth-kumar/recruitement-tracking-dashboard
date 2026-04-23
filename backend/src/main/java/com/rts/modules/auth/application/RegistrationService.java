package com.rts.modules.auth.application;

import com.rts.modules.auth.api.dto.RegisterUserRequest;
import com.rts.modules.auth.domain.Role;
import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.UserRepository;
import com.rts.shared.exception.ConflictException;
import com.rts.shared.exception.ValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterUserRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.findByUsername(username).isPresent()) {
            throw new ConflictException("Username already in use");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email already in use");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new ValidationException("Password and confirm password do not match");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.RECRUITER);

        return userRepository.save(user);
    }
}
