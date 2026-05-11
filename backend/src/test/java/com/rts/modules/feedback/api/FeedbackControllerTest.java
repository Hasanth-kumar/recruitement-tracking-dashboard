package com.rts.modules.feedback.api;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
}
