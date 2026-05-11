package com.rts.modules.feedback.persistence;

import com.rts.modules.feedback.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, String> {

    Optional<Feedback> findByInterviewIdAndSubmittedByUsernameIgnoreCaseAndDeletedFalse(
            String interviewId,
            String submittedByUsername
    );

    @Query("""
            select avg(
                (f.technicalRating + f.communicationRating + f.problemSolvingRating
                    + f.leadershipRating + f.cultureRating) / 5.0
            )
            from Feedback f
            where f.candidateId = :candidateId and f.deleted = false
            """)
    Double averagePerFeedbackScore(@Param("candidateId") String candidateId);
}
