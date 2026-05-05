package com.rts.modules.candidate.application;

import com.rts.modules.candidate.api.dto.CandidateResponse;
import com.rts.modules.candidate.api.dto.CreateCandidateRequest;
import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.domain.CandidateDocumentType;
import com.rts.modules.candidate.persistence.CandidateDocumentRepository;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.shared.exception.ConflictException;
import com.rts.shared.kernel.RecruitmentStage;
import com.rts.shared.response.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private CandidateDocumentRepository candidateDocumentRepository;

    private CandidateService candidateService;

    @BeforeEach
    void setUp() {
        candidateService = new CandidateService(candidateRepository, candidateDocumentRepository);
        lenient().when(candidateDocumentRepository.findCandidateIdsWithDocumentType(
                any(CandidateDocumentType.class),
                anyCollection()
        )).thenReturn(List.of());
    }

    @Test
    void createShouldPersistCandidateWithApplicationReceivedStage() {
        CreateCandidateRequest request = new CreateCandidateRequest(
                "  Aisha Khan  ",
                "  AISHA@RTS.COM  ",
                "  +919876543210  ",
                "  Backend Engineer  ",
                null,
                null
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
                "Backend Engineer",
                null,
                null
        );
        when(candidateRepository.existsByEmailIgnoreCaseAndDeletedFalse("aisha@rts.com")).thenReturn(true);

        assertThatThrownBy(() -> candidateService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Candidate with this email already exists");
    }

    @Test
    void listShouldMapPageToPagedResponse() {
        Candidate candidate = new Candidate();
        candidate.setId("c-1");
        candidate.setName("Aisha Khan");
        candidate.setEmail("aisha@rts.com");
        candidate.setPhone("+919876543210");
        candidate.setPosition("Backend Engineer");
        candidate.setStage(RecruitmentStage.APPLICATION_RECEIVED);

        Page<Candidate> page = new PageImpl<>(List.of(candidate), PageRequest.of(0, 20), 1);
        when(candidateRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<CandidateResponse> result = candidateService.list(null, null, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).id()).isEqualTo("c-1");
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
    }

    @Test
    void listShouldCapPageSizeAtOneHundred() {
        when(candidateRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable p = invocation.getArgument(1);
                    return new PageImpl<Candidate>(List.of(), p, 0);
                });

        PagedResponse<CandidateResponse> result = candidateService.list(
                null,
                null,
                PageRequest.of(0, 500)
        );

        assertThat(result.size()).isEqualTo(100);
    }

    @Test
    void listShouldIgnoreDisallowedSortProperties() {
        when(candidateRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable p = invocation.getArgument(1);
                    assertThat(p.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
                    return new PageImpl<Candidate>(List.of(), p, 0);
                });

        candidateService.list(
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Order.asc("invalidProperty")))
        );
    }
}
