package com.rts.modules.candidate.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class CandidatePrePersistCallbackTest {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE
    );

    @Test
    void onCreateShouldAssignUuidWhenIdIsNull() {
        Candidate candidate = newCandidateWithoutId();
        ReflectionTestUtils.invokeMethod(candidate, "onCreate");

        assertThat(candidate.getId()).isNotBlank().matches(UUID_PATTERN);
        assertThat(candidate.getCreatedAt()).isNotNull();
        assertThat(candidate.getUpdatedAt()).isNotNull();
    }

    @Test
    void onCreateShouldPreserveExplicitId() {
        String existing = UUID.randomUUID().toString();
        Candidate candidate = newCandidateWithoutId();
        candidate.setId(existing);

        ReflectionTestUtils.invokeMethod(candidate, "onCreate");

        assertThat(candidate.getId()).isEqualTo(existing);
    }

    private static Candidate newCandidateWithoutId() {
        Candidate candidate = new Candidate();
        candidate.setName("Test User");
        candidate.setEmail("test@rts.com");
        candidate.setPhone("+919876543210");
        candidate.setPosition("Engineer");
        return candidate;
    }
}
