package com.rts.modules.interview.application;

import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewPhoto;
import com.rts.modules.interview.persistence.InterviewPhotoRepository;
import com.rts.modules.interview.persistence.InterviewRepository;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class InterviewPhotoService {

    private static final long MAX_PHOTO_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_PHOTOS_PER_INTERVIEW = 10;
    private static final Set<String> ALLOWED_PHOTO_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final InterviewRepository interviewRepository;
    private final InterviewPhotoRepository interviewPhotoRepository;
    private final Path uploadRootPath;

    public InterviewPhotoService(
            InterviewRepository interviewRepository,
            InterviewPhotoRepository interviewPhotoRepository,
            @Value("${rts.storage.upload-dir:uploads}") String uploadDir
    ) {
        this.interviewRepository = interviewRepository;
        this.interviewPhotoRepository = interviewPhotoRepository;
        this.uploadRootPath = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER', 'INTERVIEWER')")
    @Transactional(readOnly = true)
    public List<InterviewPhoto> listPhotosForInterview(String interviewId) {
        interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));
        return interviewPhotoRepository.findByInterviewIdAndDeletedFalseOrderByUploadedAtAsc(interviewId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER', 'INTERVIEWER')")
    @Transactional
    public List<InterviewPhoto> uploadPhotos(String interviewId, List<MultipartFile> files, List<String> captions) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));

        if (files == null || files.isEmpty()) {
            throw new ValidationException("At least one photo file is required");
        }

        if (files.size() > MAX_PHOTOS_PER_INTERVIEW) {
            throw new ValidationException("Cannot upload more than 10 photos in a single request");
        }

        long existingCount = interviewPhotoRepository.countByInterviewIdAndDeletedFalse(interviewId);
        if (existingCount + files.size() > MAX_PHOTOS_PER_INTERVIEW) {
            throw new ValidationException("Maximum 10 photos are allowed per interview");
        }

        List<InterviewPhoto> toSave = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            validatePhotoFile(file);

            String originalFilename = Optional.ofNullable(file.getOriginalFilename())
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .orElseThrow(() -> new ValidationException("Photo file name is required"));

            String extension = getExtension(originalFilename);
            String normalizedExtension = extension.equals("jpeg") ? "jpg" : extension;

            String storedFileName = UUID.randomUUID() + "." + normalizedExtension;
            Path targetDirectory = uploadRootPath.resolve("interview-photos").resolve(interviewId).normalize();
            Path targetFile = targetDirectory.resolve(storedFileName).normalize();
            if (!targetFile.startsWith(uploadRootPath)) {
                throw new ValidationException("Invalid file upload path");
            }

            try {
                Files.createDirectories(targetDirectory);
                Files.copy(file.getInputStream(), targetFile);
            } catch (IOException ex) {
                throw new ValidationException("Unable to store photo file");
            }

            InterviewPhoto photo = new InterviewPhoto();
            photo.setInterviewId(interview.getId());
            photo.setOriginalFileName(originalFilename);
            photo.setStoredFileName(storedFileName);
            photo.setFilePath(targetFile.toString());
            photo.setContentType(resolveContentType(normalizedExtension));
            photo.setFileSize(file.getSize());
            photo.setCaption(trimToNull(captionAt(captions, index)));
            photo.setUploadedAt(LocalDateTime.now());
            toSave.add(photo);
        }

        return interviewPhotoRepository.saveAll(toSave);
    }

    private String captionAt(List<String> captions, int index) {
        if (captions == null || captions.size() <= index) {
            return null;
        }
        return captions.get(index);
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
            throw new ValidationException("Photo file size must not exceed 5MB");
        }
    }

    private String getExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == filename.length() - 1) {
            throw new ValidationException("File extension is required");
        }
        return filename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String resolveContentType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> throw new ValidationException("Only JPG and PNG files are allowed");
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
