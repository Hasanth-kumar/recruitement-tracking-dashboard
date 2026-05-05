package com.rts.modules.candidate.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CreateCandidateRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void validRequestShouldHaveNoViolations() {
        CreateCandidateRequest request = new CreateCandidateRequest(
                "Aisha Khan",
                "aisha@rts.com",
                "+919876543210",
                "Backend Engineer",
                "3 - 5 years",
                "Notes here"
        );
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void blankNameShouldFailValidation() {
        CreateCandidateRequest request = new CreateCandidateRequest(
                "   ",
                "aisha@rts.com",
                "+919876543210",
                "Backend Engineer",
                null,
                null
        );
        assertThat(propertyPaths(validator.validate(request))).contains("name");
    }

    @Test
    void invalidEmailShouldFailValidation() {
        CreateCandidateRequest request = new CreateCandidateRequest(
                "Aisha Khan",
                "not-an-email",
                "+919876543210",
                "Backend Engineer",
                null,
                null
        );
        assertThat(propertyPaths(validator.validate(request))).contains("email");
    }

    @Test
    void blankEmailShouldFailValidation() {
        CreateCandidateRequest request = new CreateCandidateRequest(
                "Aisha Khan",
                "   ",
                "+919876543210",
                "Backend Engineer",
                null,
                null
        );
        assertThat(propertyPaths(validator.validate(request))).contains("email");
    }

    @Test
    void invalidPhoneShouldFailValidation() {
        CreateCandidateRequest request = new CreateCandidateRequest(
                "Aisha Khan",
                "aisha@rts.com",
                "short",
                "Backend Engineer",
                null,
                null
        );
        assertThat(propertyPaths(validator.validate(request))).contains("phone");
    }

    @Test
    void blankPositionShouldFailValidation() {
        CreateCandidateRequest request = new CreateCandidateRequest(
                "Aisha Khan",
                "aisha@rts.com",
                "+919876543210",
                "",
                null,
                null
        );
        assertThat(propertyPaths(validator.validate(request))).contains("position");
    }

    @Test
    void nameExceedingMaxLengthShouldFail() {
        String longName = "x".repeat(151);
        CreateCandidateRequest request = new CreateCandidateRequest(
                longName,
                "a@b.co",
                "+919876543210",
                "Engineer",
                null,
                null
        );
        assertThat(propertyPaths(validator.validate(request))).contains("name");
    }

    private static Set<String> propertyPaths(Set<ConstraintViolation<CreateCandidateRequest>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
