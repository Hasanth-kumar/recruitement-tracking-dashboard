package com.rts.modules.interview.persistence;

import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewRound;
import com.rts.modules.interview.domain.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, String> {

    List<Interview> findByStatusInAndDateTimeBetween(Collection<InterviewStatus> statuses, LocalDateTime start, LocalDateTime end);

    boolean existsByCandidateIdAndRoundAndStatus(String candidateId, InterviewRound round, InterviewStatus status);

    @Query("""
            select distinct i
            from Interview i
            left join i.interviewerUsernames iu
            where i.status = :status
              and i.dateTime >= :fromDateTime
              and i.dateTime <= :toDateTime
              and (:interviewerUsername is null or lower(iu) = :interviewerUsername)
            order by i.dateTime asc
            """)
    List<Interview> findSchedule(
            @Param("status") InterviewStatus status,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime,
            @Param("interviewerUsername") String interviewerUsername
    );

    @Query("""
            select i
            from Interview i
            where i.deleted = false
              and i.status in :statuses
              and i.dateTime <= :scheduledBefore
              and i.dateTime >= :scheduledAfter
            """)
    List<Interview> findInterviewsScheduledBetween(
            @Param("statuses") Collection<InterviewStatus> statuses,
            @Param("scheduledBefore") LocalDateTime scheduledBefore,
            @Param("scheduledAfter") LocalDateTime scheduledAfter
    );
}
