package com.rts.modules.candidate.application;

import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.domain.CandidateDocument;
import com.rts.modules.candidate.domain.CandidateDocumentType;
import com.rts.modules.candidate.persistence.CandidateDocumentRepository;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private CandidateDocumentRepository candidateDocumentRepository;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(candidateRepository, candidateDocumentRepository, tempDir.toString());
    }

    @Test
    void uploadResumeShouldPersistDocumentForCandidate() throws Exception {
        String candidateId = "candidate-1";
        Candidate candidate = new Candidate();
        candidate.setId(candidateId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "sample".getBytes()
        );

        when(candidateRepository.findByIdAndDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateDocumentRepository.findByCandidateIdAndDocumentTypeAndDeletedFalse(
                candidateId, CandidateDocumentType.RESUME
        )).thenReturn(Optional.empty());
        when(candidateDocumentRepository.save(any(CandidateDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CandidateDocument saved = documentService.uploadResume(candidateId, file);

        assertThat(saved.getDocumentType()).isEqualTo(CandidateDocumentType.RESUME);
        assertThat(saved.getCandidate()).isEqualTo(candidate);
        assertThat(saved.getOriginalFileName()).isEqualTo("resume.pdf");
        assertThat(saved.getFileSize()).isEqualTo(file.getSize());
        assertThat(saved.getFilePath()).contains("resumes");
        assertThat(Files.exists(Path.of(saved.getFilePath()))).isTrue();
    }

    @Test
    void uploadResumeShouldFailForUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.txt",
                "text/plain",
                "sample".getBytes()
        );

        assertThatThrownBy(() -> documentService.uploadResume("candidate-1", file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Only PDF, DOC, and DOCX files are allowed");
    }

    @Test
    void uploadResumeShouldFailWhenFileExceedsFiveMb() {
        byte[] tooLarge = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                tooLarge
        );

        assertThatThrownBy(() -> documentService.uploadResume("candidate-1", file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Resume file size must not exceed 5MB");
    }

    @Test
    void uploadResumeShouldFailWhenCandidateDoesNotExist() {
        String candidateId = "missing";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.doc",
                "application/msword",
                "sample".getBytes()
        );
        when(candidateRepository.findByIdAndDeletedFalse(candidateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.uploadResume(candidateId, file))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Candidate not found: missing");
    }

    @Test
    void uploadResumeShouldReuseExistingResumeDocumentRecord() {
        String candidateId = "candidate-1";
        Candidate candidate = new Candidate();
        candidate.setId(candidateId);
        CandidateDocument existing = new CandidateDocument();
        existing.setId("doc-1");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.doc",
                "application/msword",
                "sample".getBytes()
        );

        when(candidateRepository.findByIdAndDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateDocumentRepository.findByCandidateIdAndDocumentTypeAndDeletedFalse(
                candidateId, CandidateDocumentType.RESUME
        )).thenReturn(Optional.of(existing));
        when(candidateDocumentRepository.save(any(CandidateDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        documentService.uploadResume(candidateId, file);

        ArgumentCaptor<CandidateDocument> captor = ArgumentCaptor.forClass(CandidateDocument.class);
        verify(candidateDocumentRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("doc-1");
    }

    @Test
    void uploadPhotoShouldPersistCompressedPhotoDocument() throws Exception {
        String candidateId = "candidate-1";
        Candidate candidate = new Candidate();
        candidate.setId(candidateId);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                createImageBytes("png", 120, 120)
        );

        when(candidateRepository.findByIdAndDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateDocumentRepository.findByCandidateIdAndDocumentTypeAndDeletedFalse(
                candidateId, CandidateDocumentType.PHOTO
        )).thenReturn(Optional.empty());
        when(candidateDocumentRepository.save(any(CandidateDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CandidateDocument saved = documentService.uploadPhoto(candidateId, file);

        assertThat(saved.getDocumentType()).isEqualTo(CandidateDocumentType.PHOTO);
        assertThat(saved.getCandidate()).isEqualTo(candidate);
        assertThat(saved.getOriginalFileName()).isEqualTo("profile.png");
        assertThat(saved.getFilePath()).contains("photos");
        assertThat(saved.getFileSize()).isLessThanOrEqualTo(2L * 1024 * 1024);
        assertThat(Files.exists(Path.of(saved.getFilePath()))).isTrue();
    }

    @Test
    void uploadPhotoShouldFailForUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.gif",
                "image/gif",
                "sample".getBytes()
        );

        assertThatThrownBy(() -> documentService.uploadPhoto("candidate-1", file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Only JPG and PNG files are allowed");
    }

    @Test
    void uploadPhotoShouldFailWhenSizeExceedsTwoMb() {
        byte[] tooLarge = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.jpg",
                "image/jpeg",
                tooLarge
        );

        assertThatThrownBy(() -> documentService.uploadPhoto("candidate-1", file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Photo file size must not exceed 2MB");
    }

    private byte[] createImageBytes(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, 0x00AEEF);
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, format, outputStream);
        return outputStream.toByteArray();
    }
}
