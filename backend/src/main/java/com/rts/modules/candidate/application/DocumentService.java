package com.rts.modules.candidate.application;

import com.rts.modules.candidate.domain.Candidate;
import com.rts.modules.candidate.domain.CandidateDocument;
import com.rts.modules.candidate.domain.CandidateDocumentType;
import com.rts.modules.candidate.persistence.CandidateDocumentRepository;
import com.rts.modules.candidate.persistence.CandidateRepository;
import com.rts.shared.exception.ResourceNotFoundException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import com.rts.shared.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final long MAX_RESUME_SIZE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PHOTO_SIZE_BYTES = 2L * 1024 * 1024;
    private static final float PHOTO_JPEG_QUALITY = 0.8f;
    private static final Set<String> ALLOWED_RESUME_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Set<String> ALLOWED_PHOTO_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final CandidateRepository candidateRepository;
    private final CandidateDocumentRepository candidateDocumentRepository;
    private final Path uploadRootPath;

    public DocumentService(
            CandidateRepository candidateRepository,
            CandidateDocumentRepository candidateDocumentRepository,
            @Value("${rts.storage.upload-dir:uploads}") String uploadDir
    ) {
        this.candidateRepository = candidateRepository;
        this.candidateDocumentRepository = candidateDocumentRepository;
        this.uploadRootPath = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional
    public CandidateDocument uploadResume(String candidateId, MultipartFile resumeFile) {
        validateResumeFile(resumeFile);

        Candidate candidate = candidateRepository.findByIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        String originalFilename = Optional.ofNullable(resumeFile.getOriginalFilename())
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new ValidationException("Resume file name is required"));

        String extension = getExtension(originalFilename);
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path targetDirectory = uploadRootPath.resolve("resumes").resolve(candidateId).normalize();
        Path targetFile = targetDirectory.resolve(storedFileName).normalize();
        if (!targetFile.startsWith(uploadRootPath)) {
            throw new ValidationException("Invalid file upload path");
        }

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(resumeFile.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ValidationException("Unable to store resume file");
        }

        CandidateDocument document = candidateDocumentRepository
                .findByCandidateIdAndDocumentTypeAndDeletedFalse(candidateId, CandidateDocumentType.RESUME)
                .orElseGet(CandidateDocument::new);

        document.setCandidate(candidate);
        document.setDocumentType(CandidateDocumentType.RESUME);
        document.setOriginalFileName(originalFilename);
        document.setStoredFileName(storedFileName);
        document.setFilePath(targetFile.toString());
        document.setContentType(Optional.ofNullable(resumeFile.getContentType()).orElse("application/octet-stream"));
        document.setFileSize(resumeFile.getSize());
        document.setLinkedAt(LocalDateTime.now());

        return candidateDocumentRepository.save(document);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional
    public CandidateDocument uploadPhoto(String candidateId, MultipartFile photoFile) {
        validatePhotoFile(photoFile);

        Candidate candidate = candidateRepository.findByIdAndDeletedFalse(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        String originalFilename = Optional.ofNullable(photoFile.getOriginalFilename())
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new ValidationException("Photo file name is required"));

        String extension = getExtension(originalFilename);
        String normalizedExtension = extension.equals("jpeg") ? "jpg" : extension;
        String storedFileName = UUID.randomUUID() + "." + normalizedExtension;
        Path targetDirectory = uploadRootPath.resolve("photos").resolve(candidateId).normalize();
        Path targetFile = targetDirectory.resolve(storedFileName).normalize();
        if (!targetFile.startsWith(uploadRootPath)) {
            throw new ValidationException("Invalid file upload path");
        }

        byte[] compressedBytes = compressPhoto(photoFile, normalizedExtension);
        if (compressedBytes.length > MAX_PHOTO_SIZE_BYTES) {
            throw new ValidationException("Photo file size must not exceed 2MB after compression");
        }

        try {
            Files.createDirectories(targetDirectory);
            Files.write(targetFile, compressedBytes);
        } catch (IOException ex) {
            throw new ValidationException("Unable to store photo file");
        }

        CandidateDocument document = candidateDocumentRepository
                .findByCandidateIdAndDocumentTypeAndDeletedFalse(candidateId, CandidateDocumentType.PHOTO)
                .orElseGet(CandidateDocument::new);

        document.setCandidate(candidate);
        document.setDocumentType(CandidateDocumentType.PHOTO);
        document.setOriginalFileName(originalFilename);
        document.setStoredFileName(storedFileName);
        document.setFilePath(targetFile.toString());
        document.setContentType("image/" + normalizedExtension);
        document.setFileSize(compressedBytes.length);
        document.setLinkedAt(LocalDateTime.now());

        return candidateDocumentRepository.save(document);
    }

    private void validateResumeFile(MultipartFile resumeFile) {
        if (resumeFile == null || resumeFile.isEmpty()) {
            throw new ValidationException("Resume file is required");
        }

        String originalFilename = Optional.ofNullable(resumeFile.getOriginalFilename())
                .map(String::trim)
                .orElse("");
        if (originalFilename.isBlank()) {
            throw new ValidationException("Resume file name is required");
        }

        String extension = getExtension(originalFilename);
        if (!ALLOWED_RESUME_EXTENSIONS.contains(extension)) {
            throw new ValidationException("Only PDF, DOC, and DOCX files are allowed");
        }

        if (resumeFile.getSize() > MAX_RESUME_SIZE_BYTES) {
            throw new ValidationException("Resume file size must not exceed 5MB");
        }
    }

    private void validatePhotoFile(MultipartFile photoFile) {
        if (photoFile == null || photoFile.isEmpty()) {
            throw new ValidationException("Photo file is required");
        }

        String originalFilename = Optional.ofNullable(photoFile.getOriginalFilename())
                .map(String::trim)
                .orElse("");
        if (originalFilename.isBlank()) {
            throw new ValidationException("Photo file name is required");
        }

        String extension = getExtension(originalFilename);
        if (!ALLOWED_PHOTO_EXTENSIONS.contains(extension)) {
            throw new ValidationException("Only JPG and PNG files are allowed");
        }

        if (photoFile.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new ValidationException("Photo file size must not exceed 2MB");
        }
    }

    private byte[] compressPhoto(MultipartFile photoFile, String extension) {
        try (InputStream inputStream = photoFile.getInputStream()) {
            BufferedImage sourceImage = ImageIO.read(inputStream);
            if (sourceImage == null) {
                throw new ValidationException("Invalid image file");
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String format = extension.equals("jpg") ? "jpeg" : "png";
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
            if (!writers.hasNext()) {
                throw new ValidationException("Unsupported image format");
            }
            ImageWriter writer = writers.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                if (extension.equals("jpg")) {
                    writeParam.setCompressionQuality(PHOTO_JPEG_QUALITY);
                } else {
                    writeParam.setCompressionQuality(0.6f);
                }
            }

            try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
                writer.setOutput(imageOutputStream);
                writer.write(null, new IIOImage(sourceImage, null, null), writeParam);
            } finally {
                writer.dispose();
            }

            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new ValidationException("Unable to process photo file");
        }
    }

    private String getExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == filename.length() - 1) {
            throw new ValidationException("File extension is required");
        }
        return filename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }

    public record ServedDocument(Resource resource, String contentType, String originalFileName) {
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional(readOnly = true)
    public ServedDocument loadPhoto(String candidateId) {
        return loadDocument(candidateId, CandidateDocumentType.PHOTO);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER')")
    @Transactional(readOnly = true)
    public ServedDocument loadResume(String candidateId) {
        return loadDocument(candidateId, CandidateDocumentType.RESUME);
    }

    private ServedDocument loadDocument(String candidateId, CandidateDocumentType type) {
        CandidateDocument document = candidateDocumentRepository
                .findByCandidateIdAndDocumentTypeAndDeletedFalse(candidateId, type)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found for candidate: " + candidateId));
        Path path = Path.of(document.getFilePath()).normalize();
        if (!path.startsWith(uploadRootPath) || !Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("Document file not found for candidate: " + candidateId);
        }
        Resource resource = new FileSystemResource(path);
        return new ServedDocument(resource, document.getContentType(), document.getOriginalFileName());
    }
}
