package com.rts.modules.candidate.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rts.modules.candidate.api.dto.CandidateResponse;
import com.rts.modules.candidate.api.dto.CreateCandidateRequest;
import com.rts.modules.candidate.api.dto.StageHistoryResponse;
import com.rts.modules.candidate.application.CandidateService;
import com.rts.modules.candidate.domain.Candidate;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.GlobalExceptionHandler;
import com.rts.shared.kernel.RecruitmentStage;
import com.rts.shared.response.PagedResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CandidateController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CandidateControllerTest {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CandidateService candidateService;

    @Test
    void createShouldReturn201WithPayloadAndUuid() throws Exception {
        String id = UUID.randomUUID().toString();
        Candidate candidate = new Candidate();
        candidate.setId(id);
        candidate.setName("Aisha Khan");
        candidate.setEmail("aisha@rts.com");
        candidate.setPhone("+919876543210");
        candidate.setPosition("Backend Engineer");
        candidate.setStage(RecruitmentStage.APPLICATION_RECEIVED);

        when(candidateService.create(any(CreateCandidateRequest.class))).thenReturn(candidate);

        CreateCandidateRequest body = new CreateCandidateRequest(
                "Aisha Khan",
                "aisha@rts.com",
                "+919876543210",
                "Backend Engineer",
                null,
                null
        );

        mockMvc.perform(post("/api/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Candidate created successfully"))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.name").value("Aisha Khan"))
                .andExpect(jsonPath("$.data.email").value("aisha@rts.com"))
                .andExpect(jsonPath("$.data.stage").value("APPLICATION_RECEIVED"));

        assertThat(id).matches(UUID_PATTERN);
    }

    @Test
    void createShouldReturn400WhenBodyFailsBeanValidation() throws Exception {
        String json = """
                {"name":"","email":"not-email","phone":"x","position":""}
                """;

        mockMvc.perform(post("/api/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createShouldReturn400WhenBodyIsNotJson() throws Exception {
        mockMvc.perform(post("/api/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listShouldReturn200WithPagedCandidates() throws Exception {
        CandidateResponse row = new CandidateResponse(
                UUID.randomUUID().toString(),
                "Aisha Khan",
                "aisha@rts.com",
                "+919876543210",
                "Backend Engineer",
                RecruitmentStage.APPLICATION_RECEIVED,
                "",
                null,
                false,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        PagedResponse<CandidateResponse> page = new PagedResponse<>(List.of(row), 0, 20, 1, 1, true, true);
        when(candidateService.list(isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Aisha Khan"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void listShouldPassStageAndPositionFiltersToService() throws Exception {
        PagedResponse<CandidateResponse> page = new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true);
        when(candidateService.list(
                eq(RecruitmentStage.SHORTLISTED),
                eq("Engineer"),
                any()
        )).thenReturn(page);

        mockMvc.perform(get("/api/candidates")
                        .param("stage", "SHORTLISTED")
                        .param("position", "Engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void getStageHistoryShouldReturn200WithData() throws Exception {
        when(candidateService.getStageHistory("candidate-1")).thenReturn(List.of(
                new StageHistoryResponse(
                        RecruitmentStage.SHORTLISTED,
                        LocalDateTime.of(2026, 5, 5, 10, 30),
                        "recruiter1"
                )
        ));

        mockMvc.perform(get("/api/candidates/candidate-1/stage-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Stage history retrieved successfully"))
                .andExpect(jsonPath("$.data[0].stage").value("SHORTLISTED"))
                .andExpect(jsonPath("$.data[0].changedBy").value("recruiter1"));
    }

    @Test
    void getStageHistoryShouldReturn404WhenCandidateMissing() throws Exception {
        when(candidateService.getStageHistory("missing"))
                .thenThrow(new ResourceNotFoundException("Candidate not found: missing"));

        mockMvc.perform(get("/api/candidates/missing/stage-history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Candidate not found: missing"));
    }

    @Test
    void updateStageShouldReturn400ForInvalidStagePayload() throws Exception {
        String invalidBody = """
                {"stage":"NOT_A_VALID_STAGE"}
                """;

        mockMvc.perform(put("/api/candidates/candidate-1/stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
