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
import static org.mockito.Mockito.never;
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
    void uploadResumeShouldFailWhenResumeFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThatThrownBy(() -> documentService.uploadResume("candidate-1", file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Resume file is required");
    }

    @Test
    void uploadResumeShouldFailWhenFileNameHasNoExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume",
                "application/pdf",
                "sample".getBytes()
        );

        assertThatThrownBy(() -> documentService.uploadResume("candidate-1", file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("File extension is required");
    }

    @Test
    void uploadResumeShouldAcceptDocxExtension() throws Exception {
        String candidateId = "candidate-1";
        Candidate candidate = new Candidate();
        candidate.setId(candidateId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "sample".getBytes()
        );

        when(candidateRepository.findByIdAndDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateDocumentRepository.findByCandidateIdAndDocumentTypeAndDeletedFalse(
                candidateId, CandidateDocumentType.RESUME
        )).thenReturn(Optional.empty());
        when(candidateDocumentRepository.save(any(CandidateDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CandidateDocument saved = documentService.uploadResume(candidateId, file);

        assertThat(saved.getOriginalFileName()).isEqualTo("resume.docx");
        assertThat(saved.getFilePath()).endsWith(".docx");
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
    void uploadPhotoShouldFailWhenPhotoFileIsEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                new byte[0]
        );

        assertThatThrownBy(() -> documentService.uploadPhoto("candidate-1", file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Photo file is required");
    }

    @Test
    void uploadPhotoShouldFailWhenImageContentIsInvalid() {
        String candidateId = "candidate-1";
        Candidate candidate = new Candidate();
        candidate.setId(candidateId);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                "not-an-image".getBytes()
        );

        when(candidateRepository.findByIdAndDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));

        assertThatThrownBy(() -> documentService.uploadPhoto(candidateId, file))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Invalid image file");
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

    @Test
    void uploadPhotoShouldFailWhenCandidateDoesNotExist() throws Exception {
        String candidateId = "missing";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                createImageBytes("png", 40, 40)
        );
        when(candidateRepository.findByIdAndDeletedFalse(candidateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.uploadPhoto(candidateId, file))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Candidate not found: missing");
    }

    @Test
    void uploadPhotoShouldReuseExistingPhotoDocumentRecord() throws Exception {
        String candidateId = "candidate-1";
        Candidate candidate = new Candidate();
        candidate.setId(candidateId);
        CandidateDocument existing = new CandidateDocument();
        existing.setId("photo-doc-1");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.jpg",
                "image/jpeg",
                createImageBytes("jpeg", 80, 80)
        );

        when(candidateRepository.findByIdAndDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateDocumentRepository.findByCandidateIdAndDocumentTypeAndDeletedFalse(
                candidateId, CandidateDocumentType.PHOTO
        )).thenReturn(Optional.of(existing));
        when(candidateDocumentRepository.save(any(CandidateDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        documentService.uploadPhoto(candidateId, file);

        ArgumentCaptor<CandidateDocument> captor = ArgumentCaptor.forClass(CandidateDocument.class);
        verify(candidateDocumentRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("photo-doc-1");
    }

    @Test
    void deletePhotoShouldSoftDeleteDocumentAndRemoveFile() throws Exception {
        String candidateId = "candidate-1";
        Candidate candidate = new Candidate();
        candidate.setId(candidateId);

        Path photoFile = tempDir.resolve("photos").resolve(candidateId).resolve("old.png").normalize();
        Files.createDirectories(photoFile.getParent());
        Files.write(photoFile, createImageBytes("png", 4, 4));

        CandidateDocument existing = new CandidateDocument();
        existing.setId("photo-doc-1");
        existing.setFilePath(photoFile.toString());

        when(candidateRepository.findByIdAndDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateDocumentRepository.findByCandidateIdAndDocumentTypeAndDeletedFalse(
                candidateId, CandidateDocumentType.PHOTO
        )).thenReturn(Optional.of(existing));
        when(candidateDocumentRepository.save(any(CandidateDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        documentService.deletePhoto(candidateId);

        assertThat(existing.isDeleted()).isTrue();
        assertThat(Files.exists(photoFile)).isFalse();
        verify(candidateDocumentRepository).save(existing);
    }

    @Test
    void deletePhotoShouldNoopWhenNoPhotoDocument() {
        String candidateId = "candidate-1";
        Candidate candidate = new Candidate();
        candidate.setId(candidateId);

        when(candidateRepository.findByIdAndDeletedFalse(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateDocumentRepository.findByCandidateIdAndDocumentTypeAndDeletedFalse(
                candidateId, CandidateDocumentType.PHOTO
        )).thenReturn(Optional.empty());

        documentService.deletePhoto(candidateId);

        verify(candidateDocumentRepository, never()).save(any());
    }

    @Test
    void deletePhotoShouldFailWhenCandidateDoesNotExist() {
        when(candidateRepository.findByIdAndDeletedFalse("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.deletePhoto("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Candidate not found: missing");
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
