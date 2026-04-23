package com.rts.modules.candidate.application;

import com.rts.modules.candidate.api.dto.CreateCandidateRequest;
import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.shared.exception.ConflictException;
import com.rts.shared.kernel.RecruitmentStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    private CandidateService candidateService;

    @BeforeEach
    void setUp() {
        candidateService = new CandidateService(candidateRepository);
    }

    @Test
    void createShouldPersistCandidateWithApplicationReceivedStage() {
        CreateCandidateRequest request = new CreateCandidateRequest(
                "  Aisha Khan  ",
                "  AISHA@RTS.COM  ",
                "  +919876543210  ",
                "  Backend Engineer  "
        );
        when(candidateRepository.existsByEmailIgnoreCaseAndDeletedFalse("aisha@rts.com")).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> {
            Candidate candidate = invocation.getArgument(0);
            candidate.setId("generated-id-1");
            return candidate;
        });

        Candidate created = candidateService.create(request);

        assertThat(created.getName()).isEqualTo("Aisha Khan");
        assertThat(created.getEmail()).isEqualTo("aisha@rts.com");
        assertThat(created.getPhone()).isEqualTo("+919876543210");
        assertThat(created.getPosition()).isEqualTo("Backend Engineer");
        assertThat(created.getStage()).isEqualTo(RecruitmentStage.APPLICATION_RECEIVED);
        assertThat(created.getId()).isEqualTo("generated-id-1");
        verify(candidateRepository).existsByEmailIgnoreCaseAndDeletedFalse(eq("aisha@rts.com"));
    }

    @Test
    void createShouldFailForDuplicateEmail() {
        CreateCandidateRequest request = new CreateCandidateRequest(
                "Aisha Khan",
                "aisha@rts.com",
                "+919876543210",
                "Backend Engineer"
        );
        when(candidateRepository.existsByEmailIgnoreCaseAndDeletedFalse("aisha@rts.com")).thenReturn(true);

        assertThatThrownBy(() -> candidateService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Candidate with this email already exists");
    }
}
