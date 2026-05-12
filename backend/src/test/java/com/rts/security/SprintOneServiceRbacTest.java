package com.rts.security;

import com.rts.modules.auth.api.dto.UpdateUserProfileRequest;
import com.rts.infrastructure.security.JwtService;
import com.rts.modules.auth.application.AuthService;
import com.rts.modules.auth.application.UserService;
import com.rts.modules.auth.domain.Role;
import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.UserRepository;
import com.rts.modules.candidate.api.dto.CreateCandidateRequest;
import com.rts.modules.candidate.api.dto.UpdateStageRequest;
import com.rts.modules.candidate.application.CandidateService;
import com.rts.modules.candidate.application.DocumentService;
import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.persistence.CandidateDocumentRepository;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.modules.candidate.persistence.StageHistoryRepository;
import com.rts.shared.kernel.RecruitmentStage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(SprintOneServiceRbacTest.TestConfig.class)
class SprintOneServiceRbacTest {

    @Autowired
    private CandidateService candidateService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private UserService userService;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private CandidateDocumentRepository candidateDocumentRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recruiterCanCreateCandidate() {
        authenticateAs(Role.RECRUITER);
        Candidate created = new Candidate();
        created.setId("candidate-1");
        created.setName("Aisha Khan");
        when(candidateRepository.existsByEmailIgnoreCaseAndDeletedFalse(anyString())).thenReturn(false);
        when(candidateRepository.save(any(Candidate.class))).thenReturn(created);

        CreateCandidateRequest request = new CreateCandidateRequest(
                "Aisha Khan",
                "aisha@rts.com",
                "+919876543210",
                "Backend Engineer",
                null,
                null
        );

        assertThatCode(() -> candidateService.create(request)).doesNotThrowAnyException();
    }

    @Test
    void interviewerCannotCreateCandidate() {
        authenticateAs(Role.INTERVIEWER);
        CreateCandidateRequest request = new CreateCandidateRequest(
                "Aisha Khan",
                "aisha@rts.com",
                "+919876543210",
                "Backend Engineer",
                null,
                null
        );
        assertThatThrownBy(() -> candidateService.create(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void interviewerCannotListCandidates() {
        authenticateAs(Role.INTERVIEWER);
        assertThatThrownBy(() -> candidateService.list(null, null, null, null, null, PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCanListCandidates() {
        authenticateAs(Role.ADMIN);
        Page<Candidate> page = new PageImpl<>(List.of());
        when(candidateRepository.findAll(
                org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Candidate>>any(),
                any(org.springframework.data.domain.Pageable.class)
        ))
                .thenReturn(page);

        assertThatCode(() -> candidateService.list(null, null, null, null, null, PageRequest.of(0, 20)))
                .doesNotThrowAnyException();
    }

    @Test
    void interviewerCannotUpdateCandidateStage() {
        authenticateAs(Role.INTERVIEWER);
        assertThatThrownBy(() -> candidateService.updateStage("candidate-1", new UpdateStageRequest(RecruitmentStage.SHORTLISTED)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void interviewerCannotUploadResume() {
        authenticateAs(Role.INTERVIEWER);
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "sample".getBytes()
        );
        assertThatThrownBy(() -> documentService.uploadResume("candidate-1", file))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void hrManagerCanUploadResume() {
        authenticateAs(Role.HR_MANAGER);
        Candidate candidate = new Candidate();
        candidate.setId("candidate-1");
        when(candidateRepository.findByIdAndDeletedFalse("candidate-1")).thenReturn(Optional.of(candidate));
        when(candidateDocumentRepository.findByCandidateIdAndDocumentTypeAndDeletedFalse(anyString(), any()))
                .thenReturn(Optional.empty());
        when(candidateDocumentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "sample".getBytes()
        );
        assertThatCode(() -> documentService.uploadResume("candidate-1", file))
                .doesNotThrowAnyException();
    }

    @Test
    void adminCanAccessAdminOnlyUserMethods() {
        authenticateAs(Role.ADMIN);
        when(userRepository.findByDeletedFalseOrderByUsernameAsc()).thenReturn(List.of());
        assertThatCode(() -> userService.listUsersForAdmin()).doesNotThrowAnyException();
    }

    @Test
    void recruiterCannotAccessAdminOnlyUserMethods() {
        authenticateAs(Role.RECRUITER);
        assertThatThrownBy(() -> userService.listUsersForAdmin())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void interviewerCanAccessProfileMethods() {
        authenticateAs(Role.INTERVIEWER);
        User user = new User();
        user.setId("user-1");
        user.setUsername("interviewer");
        user.setEmail("interviewer@rts.com");
        user.setRole(Role.INTERVIEWER);

        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "interviewer",
                "interviewer@rts.com",
                null,
                null,
                null
        );

        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                user,
                null,
                user.getAuthorities()
        );

        assertThatCode(() -> userService.getProfile(auth)).doesNotThrowAnyException();
        assertThatCode(() -> userService.updateProfile(auth, request)).doesNotThrowAnyException();
    }

    private void authenticateAs(Role role) {
        Collection<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                role.name().toLowerCase(),
                null,
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        CandidateRepository candidateRepository() {
            return Mockito.mock(CandidateRepository.class);
        }

        @Bean
        CandidateDocumentRepository candidateDocumentRepository() {
            return Mockito.mock(CandidateDocumentRepository.class);
        }

        @Bean
        StageHistoryRepository stageHistoryRepository() {
            return Mockito.mock(StageHistoryRepository.class);
        }

        @Bean
        UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        JwtService jwtService() {
            return Mockito.mock(JwtService.class);
        }

        @Bean
        AuthService authService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
            return new AuthService(userRepository, passwordEncoder, jwtService);
        }

        @Bean
        UserService userService(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
            return new UserService(authService, userRepository, passwordEncoder);
        }

        @Bean
        ApplicationEventPublisher applicationEventPublisher() {
            return Mockito.mock(ApplicationEventPublisher.class);
        }

        @Bean
        CandidateService candidateService(
                CandidateRepository candidateRepository,
                CandidateDocumentRepository candidateDocumentRepository,
                StageHistoryRepository stageHistoryRepository,
                ApplicationEventPublisher applicationEventPublisher
        ) {
            return new CandidateService(candidateRepository, candidateDocumentRepository, stageHistoryRepository, applicationEventPublisher);
        }

        @Bean
        DocumentService documentService(
                CandidateRepository candidateRepository,
                CandidateDocumentRepository candidateDocumentRepository
        ) {
            return new DocumentService(candidateRepository, candidateDocumentRepository, "target/test-uploads");
        }
    }
}
