package com.rts.modules.feedback.api;

import com.rts.modules.feedback.api.dto.CandidateFeedbackSummaryResponse;
import com.rts.modules.feedback.api.dto.FeedbackResponse;
import com.rts.modules.feedback.application.FeedbackService;
import com.rts.modules.feedback.domain.FeedbackRecommendation;
import com.rts.shared.exception.GlobalExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @Test
    void postFeedbackShouldReturn201() throws Exception {
        LocalDateTime ts = LocalDateTime.of(2026, 5, 10, 12, 0);
        FeedbackResponse body = new FeedbackResponse(
                "fb-1",
                "int-1",
                "cand-1",
                "alice",
                4,
                4,
                4,
                4,
                4,
                FeedbackRecommendation.HOLD,
                "Good depth",
                ts,
                ts,
                ts
        );
        when(feedbackService.submit(any())).thenReturn(body);

        String json = """
                {
                  "interviewId": "int-1",
                  "technicalRating": 4,
                  "communicationRating": 4,
                  "problemSolvingRating": 4,
                  "leadershipRating": 4,
                  "cultureRating": 4,
                  "recommendation": "HOLD",
                  "comments": "Good depth"
                }
                """;

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("fb-1"))
                .andExpect(jsonPath("$.data.recommendation").value("HOLD"));

        verify(feedbackService).submit(any());
    }

    @Test
    void postFeedbackShouldReturn400WhenRatingOutOfRange() throws Exception {
        String json = """
                {
                  "interviewId": "int-1",
                  "technicalRating": 6,
                  "communicationRating": 4,
                  "problemSolvingRating": 4,
                  "leadershipRating": 4,
                  "cultureRating": 4,
                  "recommendation": "PROCEED"
                }
                """;

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCandidateFeedbackShouldReturn200WithSummary() throws Exception {
        LocalDateTime ts = LocalDateTime.of(2026, 5, 10, 12, 0);
        FeedbackResponse fb = new FeedbackResponse(
                "fb-1", "int-1", "cand-1", "alice",
                5, 4, 4, 3, 4,
                FeedbackRecommendation.PROCEED, "Strong candidate", ts, ts, ts
        );
        CandidateFeedbackSummaryResponse summary = CandidateFeedbackSummaryResponse.from("cand-1", List.of(fb));
        when(feedbackService.getCandidateFeedback("cand-1")).thenReturn(summary);

        mockMvc.perform(get("/api/candidates/cand-1/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.candidateId").value("cand-1"))
                .andExpect(jsonPath("$.data.totalFeedbackCount").value(1))
                .andExpect(jsonPath("$.data.averageTechnicalRating").value(5.0))
                .andExpect(jsonPath("$.data.overallAverageRating").isNotEmpty())
                .andExpect(jsonPath("$.data.feedbacks[0].id").value("fb-1"));

        verify(feedbackService).getCandidateFeedback("cand-1");
    }

    @Test
    void getCandidateFeedbackShouldReturn200WhenNoFeedback() throws Exception {
        CandidateFeedbackSummaryResponse empty = CandidateFeedbackSummaryResponse.from("cand-999", List.of());
        when(feedbackService.getCandidateFeedback("cand-999")).thenReturn(empty);

        mockMvc.perform(get("/api/candidates/cand-999/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.candidateId").value("cand-999"))
                .andExpect(jsonPath("$.data.totalFeedbackCount").value(0))
                .andExpect(jsonPath("$.data.feedbacks").isEmpty());
    }
}
