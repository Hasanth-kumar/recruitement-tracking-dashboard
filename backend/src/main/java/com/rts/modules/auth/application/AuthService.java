package com.rts.modules.auth.application;

import com.rts.infrastructure.security.JwtService;
import com.rts.modules.auth.api.dto.LoginRequest;
import com.rts.modules.auth.api.dto.LoginResponse;
import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.usernameOrEmail(), request.usernameOrEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(
                Map.of("role", user.getRole().name()),
                user
        );

        return LoginResponse.of(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public User getAuthenticatedUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new IllegalStateException("Authenticated principal is not a RTS user");
    }
}
