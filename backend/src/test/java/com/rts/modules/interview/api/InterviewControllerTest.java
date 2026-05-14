package com.rts.modules.interview.api;

import com.rts.modules.auth.application.UserService;
import com.rts.modules.interview.api.dto.InterviewResponse;
import com.rts.modules.interview.api.dto.CancelInterviewRequest;
import com.rts.modules.interview.api.dto.RescheduleInterviewRequest;
import com.rts.modules.interview.api.dto.ScheduleRoundTwoInterviewRequest;
import com.rts.infrastructure.security.CustomUserDetailsService;
import com.rts.infrastructure.security.JwtService;
import com.rts.modules.interview.application.InterviewPhotoService;
import com.rts.modules.interview.application.InterviewService;
import com.rts.modules.interview.domain.InterviewPhoto;
import com.rts.modules.interview.domain.InterviewRound;
import com.rts.modules.interview.domain.InterviewStatus;
import com.rts.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InterviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InterviewService interviewService;

    @MockBean
    private InterviewPhotoService interviewPhotoService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getScheduleShouldRequireDateRangeParams() throws Exception {
        mockMvc.perform(get("/api/interviews/schedule"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getScheduleShouldReturnPayloadAndForwardFilters() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 5, 20, 9, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 20, 18, 0);

        InterviewResponse row = new InterviewResponse(
                "int-1",
                "candidate-1",
                InterviewRound.ROUND_2,
                LocalDateTime.of(2026, 5, 20, 14, 30),
                45,
                null,
                "Room B",
                null,
                InterviewStatus.SCHEDULED,
                List.of("alice", "bob")
        );

        when(interviewService.getSchedule(eq(from), eq(to), eq("alice")))
                .thenReturn(List.of(row));

        mockMvc.perform(get("/api/interviews/schedule")
                        .param("fromDateTime", "2026-05-20T09:00:00")
                        .param("toDateTime", "2026-05-20T18:00:00")
                        .param("interviewerUsername", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("int-1"))
                .andExpect(jsonPath("$.data[0].round").value("ROUND_2"))
                .andExpect(jsonPath("$.data[0].location").value("Room B"))
                .andExpect(jsonPath("$.data[0].interviewerUsernames[0]").value("alice"));

        verify(interviewService).getSchedule(from, to, "alice");
    }

    @Test
    void getScheduleShouldOmitInterviewerFilterWhenParamAbsent() throws Exception {
        LocalDateTime from = LocalDateTime.of(2026, 6, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 6, 1, 23, 59, 59);

        when(interviewService.getSchedule(eq(from), eq(to), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/interviews/schedule")
                        .param("fromDateTime", "2026-06-01T00:00:00")
                        .param("toDateTime", "2026-06-01T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        verify(interviewService).getSchedule(from, to, null);
    }

    @Test
    void scheduleRoundTwoShouldAcceptJsonBody() throws Exception {
        InterviewResponse response = new InterviewResponse(
                "r2-new",
                "candidate-x",
                InterviewRound.ROUND_2,
                LocalDateTime.of(2026, 7, 10, 11, 0),
                60,
                null,
                "HQ — Panel room",
                "Bring laptop",
                InterviewStatus.SCHEDULED,
                List.of("pat")
        );

        when(interviewService.scheduleRoundTwo(any(ScheduleRoundTwoInterviewRequest.class)))
                .thenReturn(response);

        String body = """
                {
                  "candidateId": "candidate-x",
                  "dateTime": "2026-07-10T11:00:00",
                  "durationMinutes": 60,
                  "location": "HQ — Panel room",
                  "interviewerUsernames": ["pat"],
                  "notes": "Bring laptop"
                }
                """;

        mockMvc.perform(post("/api/interviews/round2")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("r2-new"))
                .andExpect(jsonPath("$.data.round").value("ROUND_2"));
    }

    @Test
    void rescheduleInterviewShouldAcceptJsonBody() throws Exception {
        InterviewResponse response = new InterviewResponse(
                "int-rs-1",
                "candidate-1",
                InterviewRound.ROUND_1,
                LocalDateTime.of(2026, 7, 12, 10, 0),
                60,
                "https://meet.google.com/new-link",
                null,
                "Replanned agenda",
                InterviewStatus.SCHEDULED,
                List.of("alice", "bob")
        );

        when(interviewService.rescheduleInterview(eq("int-rs-1"), any(RescheduleInterviewRequest.class)))
                .thenReturn(response);

        String body = """
                {
                  "dateTime": "2026-07-12T10:00:00",
                  "durationMinutes": 60,
                  "interviewerUsernames": ["alice", "bob"],
                  "meetingLink": "https://meet.google.com/new-link",
                  "notes": "Replanned agenda",
                  "rescheduleReason": "Interviewer slot overlap"
                }
                """;

        mockMvc.perform(put("/api/interviews/int-rs-1/reschedule")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("int-rs-1"))
                .andExpect(jsonPath("$.data.durationMinutes").value(60));
    }

    @Test
    void cancelInterviewShouldReturnUpdatedInterview() throws Exception {
        InterviewResponse response = new InterviewResponse(
                "int-c-1",
                "candidate-1",
                InterviewRound.ROUND_1,
                LocalDateTime.of(2026, 7, 15, 16, 0),
                45,
                "https://meet.google.com/cancel-me",
                null,
                "Interview cancelled. Reason: interviewer unavailable",
                InterviewStatus.CANCELLED,
                List.of("alice")
        );

        when(interviewService.cancelInterview(eq("int-c-1"), any(CancelInterviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/interviews/int-c-1/cancel")
                        .contentType("application/json")
                        .content("""
                                {
                                  "reason": "interviewer unavailable"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("int-c-1"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void uploadInterviewPhotosShouldReturnCreatedPayload() throws Exception {
        InterviewPhoto photo = new InterviewPhoto();
        photo.setId("photo-1");
        photo.setInterviewId("int-1");
        photo.setOriginalFileName("panel.png");
        photo.setContentType("image/png");
        photo.setFileSize(1024L);
        photo.setUploadedAt(LocalDateTime.of(2026, 7, 16, 10, 0));

        when(interviewPhotoService.uploadPhotos(eq("int-1"), any(), any()))
                .thenReturn(List.of(photo));

        mockMvc.perform(multipart("/api/interviews/int-1/photos")
                        .file("files", "dummy".getBytes()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("photo-1"))
                .andExpect(jsonPath("$.data[0].interviewId").value("int-1"));
    }

}
