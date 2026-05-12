package com.rts.modules.candidate.application;

import com.rts.modules.candidate.api.dto.CandidateResponse;
import com.rts.modules.candidate.api.dto.CreateCandidateRequest;
import com.rts.modules.candidate.api.dto.UpdateCandidateRequest;
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
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private CandidateDocumentRepository candidateDocumentRepository;

    @Mock
    private StageHistoryRepository stageHistoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CandidateService candidateService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        candidateService = new CandidateService(candidateRepository, candidateDocumentRepository, stageHistoryRepository, eventPublisher);
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
        when(candidateRepository.existsByEmailIgnoreCaseAndDeletedFalse(anyString())).thenReturn(false);
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
        verify(candidateRepository).existsByEmailIgnoreCaseAndDeletedFalse(anyString());
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
        when(candidateRepository.existsByEmailIgnoreCaseAndDeletedFalse(anyString())).thenReturn(true);

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
        when(candidateRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Candidate>>any(), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<CandidateResponse> result = candidateService.list(null, null, null, null, null, PageRequest.of(0, 20));

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
        when(candidateRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Candidate>>any(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable p = invocation.getArgument(1);
                    return new PageImpl<Candidate>(List.of(), p, 0);
                });

        PagedResponse<CandidateResponse> result = candidateService.list(
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 500)
        );

        assertThat(result.size()).isEqualTo(100);
    }

    @Test
    void listShouldIgnoreDisallowedSortProperties() {
        when(candidateRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Candidate>>any(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable p = invocation.getArgument(1);
                    assertThat(p.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
                    return new PageImpl<Candidate>(List.of(), p, 0);
                });

        candidateService.list(
                null,
                null,
                null,
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

    @Test
    void getByIdShouldReturnCandidateResponse() {
        Candidate candidate = new Candidate();
        candidate.setId("c-1");
        candidate.setName("Aisha Khan");
        candidate.setEmail("aisha@rts.com");
        candidate.setPhone("+919876543210");
        candidate.setPosition("Backend Engineer");
        candidate.setStage(RecruitmentStage.APPLICATION_RECEIVED);

        when(candidateRepository.findByIdAndDeletedFalse("c-1")).thenReturn(Optional.of(candidate));
        when(candidateDocumentRepository.findByCandidateIdAndDocumentTypeAndDeletedFalse("c-1", CandidateDocumentType.PHOTO))
                .thenReturn(Optional.empty());
        when(candidateDocumentRepository.findByCandidateIdAndDocumentTypeAndDeletedFalse("c-1", CandidateDocumentType.RESUME))
                .thenReturn(Optional.empty());

        CandidateResponse response = candidateService.getById("c-1");

        assertThat(response.id()).isEqualTo("c-1");
        assertThat(response.name()).isEqualTo("Aisha Khan");
        assertThat(response.email()).isEqualTo("aisha@rts.com");
        assertThat(response.hasResume()).isFalse();
        assertThat(response.hasPhoto()).isFalse();
        assertThat(response.evalScore()).isNull();
    }

    @Test
    void getByIdShouldFailWhenCandidateMissing() {
        when(candidateRepository.findByIdAndDeletedFalse("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidateService.getById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Candidate not found: missing");
    }

    @Test
    void updateShouldNormalizeEmailAndPersist() {
        Candidate candidate = new Candidate();
        candidate.setId("c-1");
        candidate.setName("Old");
        candidate.setEmail("old@rts.com");
        candidate.setPhone("+919876543210");
        candidate.setPosition("Backend Engineer");
        candidate.setStage(RecruitmentStage.APPLICATION_RECEIVED);

        when(candidateRepository.findByIdAndDeletedFalse("c-1")).thenReturn(Optional.of(candidate));
        when(candidateRepository.existsByEmailIgnoreCaseAndDeletedFalseAndIdNot(anyString(), anyString())).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(candidateDocumentRepository.findByCandidateIdAndDocumentTypeAndDeletedFalse("c-1", CandidateDocumentType.PHOTO))
                .thenReturn(Optional.empty());
        when(candidateDocumentRepository.findByCandidateIdAndDocumentTypeAndDeletedFalse("c-1", CandidateDocumentType.RESUME))
                .thenReturn(Optional.empty());

        UpdateCandidateRequest request = new UpdateCandidateRequest(
                "  New Name  ",
                "  NEW@RTS.COM  ",
                "+919876543210",
                "Backend Engineer",
                null,
                null
        );

        CandidateResponse updated = candidateService.update("c-1", request);

        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.email()).isEqualTo("new@rts.com");
        assertThat(candidate.getEmail()).isEqualTo("new@rts.com");
    }

    @Test
    void softDeleteShouldMarkCandidateDeleted() {
        Candidate candidate = new Candidate();
        candidate.setId("c-1");
        candidate.setDeleted(false);
        when(candidateRepository.findByIdAndDeletedFalse("c-1")).thenReturn(Optional.of(candidate));

        candidateService.softDelete("c-1");

        assertThat(candidate.isDeleted()).isTrue();
    }

    @Test
    void softDeleteShouldFailWhenCandidateMissing() {
        when(candidateRepository.findByIdAndDeletedFalse("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidateService.softDelete("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Candidate not found: missing");
    }

    @Test
    void listShouldForwardSearchParametersToRepositoryLayer() {
        when(candidateRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Candidate>>any(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Candidate>(List.of(), invocation.getArgument(1), 0));

        candidateService.list(null, null, "Ali", null, null, PageRequest.of(0, 20));

        verify(candidateRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Candidate>>any(), any(Pageable.class));
    }

    @Test
    void listShouldForwardCreatedDateFiltersToRepositoryLayer() {
        when(candidateRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Candidate>>any(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Candidate>(List.of(), invocation.getArgument(1), 0));

        candidateService.list(
                null,
                null,
                null,
                java.time.LocalDate.of(2026, 5, 1),
                java.time.LocalDate.of(2026, 5, 31),
                PageRequest.of(0, 20)
        );

        verify(candidateRepository).findAll(org.mockito.ArgumentMatchers.<Specification<Candidate>>any(), any(Pageable.class));
    }

    @Test
    void bulkUpdateStageShouldUpdateOnlyChangedCandidatesAndWriteHistory() {
        Candidate c1 = new Candidate();
        c1.setId("c-1");
        c1.setStage(RecruitmentStage.APPLICATION_RECEIVED);
        Candidate c2 = new Candidate();
        c2.setId("c-2");
        c2.setStage(RecruitmentStage.SHORTLISTED);

        when(candidateRepository.findByIdInAndDeletedFalse(anyCollection())).thenReturn(List.of(c1, c2));
        when(candidateRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("hr.user", null, List.of())
        );

        var response = candidateService.bulkUpdateStage(
                List.of("c-1", "c-2"),
                RecruitmentStage.R1_SCHEDULED
        );

        assertThat(response.requestedCount()).isEqualTo(2);
        assertThat(response.updatedCount()).isEqualTo(2);
        assertThat(response.updatedCandidateIds()).containsExactly("c-1", "c-2");
        assertThat(c1.getStage()).isEqualTo(RecruitmentStage.R1_SCHEDULED);
        assertThat(c2.getStage()).isEqualTo(RecruitmentStage.R1_SCHEDULED);
        verify(stageHistoryRepository).saveAll(any());
    }

    @Test
    void bulkUpdateStageShouldSkipCandidatesAlreadyInTargetStage() {
        Candidate c1 = new Candidate();
        c1.setId("c-1");
        c1.setStage(RecruitmentStage.R1_SCHEDULED);
        Candidate c2 = new Candidate();
        c2.setId("c-2");
        c2.setStage(RecruitmentStage.SHORTLISTED);

        when(candidateRepository.findByIdInAndDeletedFalse(anyCollection())).thenReturn(List.of(c1, c2));
        when(candidateRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = candidateService.bulkUpdateStage(
                List.of("c-1", "c-2"),
                RecruitmentStage.R1_SCHEDULED
        );

        assertThat(response.requestedCount()).isEqualTo(2);
        assertThat(response.updatedCount()).isEqualTo(1);
        assertThat(response.updatedCandidateIds()).containsExactly("c-2");
    }

    @Test
    void bulkUpdateStageShouldFailWhenAnyCandidateIsMissing() {
        Candidate c1 = new Candidate();
        c1.setId("c-1");
        c1.setStage(RecruitmentStage.APPLICATION_RECEIVED);

        when(candidateRepository.findByIdInAndDeletedFalse(anyCollection())).thenReturn(List.of(c1));

        assertThatThrownBy(() -> candidateService.bulkUpdateStage(
                List.of("c-1", "missing"),
                RecruitmentStage.R1_SCHEDULED
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("One or more candidates were not found");
    }

    @Test
    void bulkUpdateStageShouldFailForBlankIdsOnly() {
        assertThatThrownBy(() -> candidateService.bulkUpdateStage(
                List.of(" ", "   "),
                RecruitmentStage.R1_SCHEDULED
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessage("At least one valid candidate ID is required");
    }
}
