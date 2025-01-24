package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.CalculationHistoryId;
import com.xmonster.howtaxing.model.CalculationSellRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationSellRequestHistoryRepository extends JpaRepository<CalculationSellRequestHistory, Long> {

    CalculationSellRequestHistory findByCalcHistoryId(Long calcHistoryId);
}
