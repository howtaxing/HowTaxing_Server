package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultRequest;
import com.xmonster.howtaxing.model.CalculationBuyRequestHistory;
import com.xmonster.howtaxing.model.CalculationHistory;
import com.xmonster.howtaxing.model.CalculationHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CalculationBuyRequestHistoryRepository extends JpaRepository<CalculationBuyRequestHistory, Long> {

    CalculationBuyRequestHistory findByCalcHistoryId(Long calcHistoryId);
}
