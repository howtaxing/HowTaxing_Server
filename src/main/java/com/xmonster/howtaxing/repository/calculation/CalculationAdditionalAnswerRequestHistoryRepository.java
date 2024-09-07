package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.CalculationAdditionalAnswerRequestHistory;
import com.xmonster.howtaxing.model.CalculationHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationAdditionalAnswerRequestHistoryRepository extends JpaRepository<CalculationAdditionalAnswerRequestHistory, CalculationHistoryId> {
}
