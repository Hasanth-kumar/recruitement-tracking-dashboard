package com.rts.infrastructure.notification;

import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserEmailResolverAdapter implements UserEmailResolverPort {

    private final UserRepository userRepository;

    public UserEmailResolverAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<String> resolveEmail(String username) {
        return userRepository.findByUsername(username).map(User::getEmail);
    }

    @Override
    public Map<String, String> resolveEmails(List<String> usernames) {
        return usernames.stream()
                .map(u -> Map.entry(u, userRepository.findByUsername(u).map(User::getEmail)))
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(), (a, b) -> a));
    }
}
