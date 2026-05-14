package com.rts.modules.interview.persistence;

import com.rts.modules.interview.domain.InterviewPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewPhotoRepository extends JpaRepository<InterviewPhoto, String> {

    long countByInterviewIdAndDeletedFalse(String interviewId);

    List<InterviewPhoto> findByInterviewIdAndDeletedFalseOrderByUploadedAtAsc(String interviewId);
}
