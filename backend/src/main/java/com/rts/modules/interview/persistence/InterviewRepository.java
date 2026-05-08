package com.rts.modules.interview.persistence;

import com.rts.modules.interview.domain.Interview;
import com.rts.modules.interview.domain.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, String> {

    List<Interview> findByStatusInAndDateTimeBetween(Collection<InterviewStatus> statuses, LocalDateTime start, LocalDateTime end);
}
