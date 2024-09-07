package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.CalculationHistoryId;
import com.xmonster.howtaxing.model.CalculationSellResponseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalculationSellResponseHistoryRepository extends JpaRepository<CalculationSellResponseHistory, CalculationHistoryId> {

    List<CalculationSellResponseHistory> findByCalculationHistoryId(CalculationHistoryId calculationHistoryId);
}
