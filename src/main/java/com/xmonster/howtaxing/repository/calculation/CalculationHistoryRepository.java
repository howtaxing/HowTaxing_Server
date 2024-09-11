package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.CalculationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationHistoryRepository extends JpaRepository<CalculationHistory, Long> {

    CalculationHistory findByCalcHistoryId(Long calcHistoryId);
}
