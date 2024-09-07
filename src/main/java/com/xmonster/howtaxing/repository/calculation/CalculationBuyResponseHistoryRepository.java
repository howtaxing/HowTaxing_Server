package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.CalculationBuyResponseHistory;
import com.xmonster.howtaxing.model.CalculationHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalculationBuyResponseHistoryRepository extends JpaRepository<CalculationBuyResponseHistory, CalculationHistoryId> {

    List<CalculationBuyResponseHistory> findByCalculationHistoryId(CalculationHistoryId calculationHistoryId);
}
