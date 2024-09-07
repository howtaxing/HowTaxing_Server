package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.CalculationBuyRequestHistory;
import com.xmonster.howtaxing.model.CalculationHistory;
import com.xmonster.howtaxing.model.CalculationHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationBuyRequestHistoryRepository extends JpaRepository<CalculationBuyRequestHistory, CalculationHistoryId> {
}
