package com.rts.infrastructure.notification;

import java.util.Map;
import java.util.List;
import java.util.Optional;

public interface UserEmailResolverPort {

    Optional<String> resolveEmail(String username);

    Map<String, String> resolveEmails(List<String> usernames);
}
