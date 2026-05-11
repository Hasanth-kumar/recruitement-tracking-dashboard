package com.rts.modules.interview.api.dto;

import com.rts.modules.interview.domain.InterviewPhoto;

import java.time.LocalDateTime;

public record InterviewPhotoUploadResponse(
        String id,
        String interviewId,
        String originalFileName,
        String contentType,
        long fileSize,
        String caption,
        LocalDateTime uploadedAt
) {
    public static InterviewPhotoUploadResponse from(InterviewPhoto photo) {
        return new InterviewPhotoUploadResponse(
                photo.getId(),
                photo.getInterviewId(),
                photo.getOriginalFileName(),
                photo.getContentType(),
                photo.getFileSize(),
                photo.getCaption(),
                photo.getUploadedAt()
        );
    }
}
