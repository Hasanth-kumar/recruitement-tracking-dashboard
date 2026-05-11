package com.rts.modules.interview.persistence;

import com.rts.modules.interview.domain.InterviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewHistoryRepository extends JpaRepository<InterviewHistory, String> {
}
