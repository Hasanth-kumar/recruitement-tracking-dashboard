package com.rts.infrastructure.config;

import com.rts.modules.auth.domain.Role;
import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUser("admin", "admin@rts.com", "Admin@123", Role.ADMIN);
        seedUser("hrmanager", "hr.manager@rts.com", "HrManager@123", Role.HR_MANAGER);
        seedUser("recruiter", "recruiter@rts.com", "Recruiter@123", Role.RECRUITER);
        seedUser("interviewer", "interviewer@rts.com", "Interviewer@123", Role.INTERVIEWER);
    }

    private void seedUser(String username, String email, String rawPassword, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);
    }
}
