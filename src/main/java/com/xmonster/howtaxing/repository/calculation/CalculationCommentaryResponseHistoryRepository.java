package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.CalculationCommentaryResponseHistory;
import com.xmonster.howtaxing.model.CalculationHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalculationCommentaryResponseHistoryRepository extends JpaRepository<CalculationCommentaryResponseHistory, CalculationHistoryId> {

    List<CalculationCommentaryResponseHistory> findByCalculationHistoryId(CalculationHistoryId calculationHistoryId);
}
