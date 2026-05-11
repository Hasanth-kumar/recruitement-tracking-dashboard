package com.rts.modules.interview.application;

import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewPhoto;
import com.rts.modules.interview.persistence.InterviewPhotoRepository;
import com.rts.modules.interview.persistence.InterviewRepository;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewPhotoServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private InterviewPhotoRepository interviewPhotoRepository;

    private InterviewPhotoService interviewPhotoService;

    @BeforeEach
    void setUp() {
        interviewPhotoService = new InterviewPhotoService(interviewRepository, interviewPhotoRepository, tempDir.toString());
    }

    @Test
    void uploadPhotosShouldStoreFilesAndPersistMetadata() throws Exception {
        Interview interview = new Interview();
        interview.setId("interview-1");
        when(interviewRepository.findById("interview-1")).thenReturn(Optional.of(interview));
        when(interviewPhotoRepository.countByInterviewIdAndDeletedFalse("interview-1")).thenReturn(0L);
        when(interviewPhotoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "panel.png",
                "image/png",
                "fake-image".getBytes()
        );

        List<InterviewPhoto> saved = interviewPhotoService.uploadPhotos("interview-1", List.of(file), List.of("Panel start"));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getInterviewId()).isEqualTo("interview-1");
        assertThat(saved.get(0).getCaption()).isEqualTo("Panel start");
        assertThat(Files.exists(Path.of(saved.get(0).getFilePath()))).isTrue();
    }

    @Test
    void uploadPhotosShouldFailWhenInterviewNotFound() {
        when(interviewRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewPhotoService.uploadPhotos("missing", List.of(), null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Interview not found: missing");
    }

    @Test
    void uploadPhotosShouldFailWhenMaxPhotosExceeded() {
        Interview interview = new Interview();
        interview.setId("interview-2");
        when(interviewRepository.findById("interview-2")).thenReturn(Optional.of(interview));
        when(interviewPhotoRepository.countByInterviewIdAndDeletedFalse("interview-2")).thenReturn(10L);

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "panel.jpg",
                "image/jpeg",
                "fake-image".getBytes()
        );

        assertThatThrownBy(() -> interviewPhotoService.uploadPhotos("interview-2", List.of(file), null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Maximum 10 photos are allowed per interview");
    }
}
