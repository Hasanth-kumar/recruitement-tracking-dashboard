package com.rts.modules.candidate.application;

import com.rts.modules.candidate.api.dto.CandidateResponse;
import com.rts.modules.candidate.api.dto.CreateCandidateRequest;
import com.rts.modules.candidate.api.dto.UpdateStageRequest;
import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.domain.CandidateDocumentType;
import com.rts.modules.candidate.domain.StageHistory;
import com.rts.modules.candidate.persistence.CandidateDocumentRepository;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.modules.candidate.persistence.StageHistoryRepository;
import com.rts.shared.exception.ConflictException;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private CandidateDocumentRepository candidateDocumentRepository;

    @Mock
    private StageHistoryRepository stageHistoryRepository;

    private CandidateService candidateService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        candidateService = new CandidateService(candidateRepository, candidateDocumentRepository, stageHistoryRepository);
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

    @Test
    void updateStageShouldCreateHistoryRow() {
        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setStage(RecruitmentStage.APPLICATION_RECEIVED);
        when(candidateRepository.findByIdAndDeletedFalse("candidate-1")).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("recruiter.user", null, List.of())
        );

        candidateService.updateStage("candidate-1", new UpdateStageRequest(RecruitmentStage.SHORTLISTED));

        assertThat(candidate.getStage()).isEqualTo(RecruitmentStage.SHORTLISTED);
        var historyCaptor = forClass(StageHistory.class);
        verify(stageHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getCandidateId()).isEqualTo("candidate-1");
        assertThat(historyCaptor.getValue().getStage()).isEqualTo(RecruitmentStage.SHORTLISTED);
        assertThat(historyCaptor.getValue().getChangedBy()).isEqualTo("recruiter.user");
        assertThat(historyCaptor.getValue().getChangedAt()).isNotNull();
    }

    @Test
    void updateStageShouldFailWhenCandidateNotFound() {
        when(candidateRepository.findByIdAndDeletedFalse("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidateService.updateStage("missing", new UpdateStageRequest(RecruitmentStage.SHORTLISTED)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Candidate not found: missing");
        verify(stageHistoryRepository, never()).save(any(StageHistory.class));
    }

    @Test
    void updateStageShouldRejectNoOp() {
        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        candidate.setStage(RecruitmentStage.SHORTLISTED);
        when(candidateRepository.findByIdAndDeletedFalse("candidate-1")).thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> candidateService.updateStage("candidate-1", new UpdateStageRequest(RecruitmentStage.SHORTLISTED)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Candidate is already in stage: SHORTLISTED");
        verify(stageHistoryRepository, never()).save(any(StageHistory.class));
    }
}
