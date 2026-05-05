package com.rts.modules.candidate.persistence;

import com.rts.modules.candidate.domain.StageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StageHistoryRepository extends JpaRepository<StageHistory, String> {

    List<StageHistory> findByCandidateIdOrderByChangedAtDesc(String candidateId);
}
