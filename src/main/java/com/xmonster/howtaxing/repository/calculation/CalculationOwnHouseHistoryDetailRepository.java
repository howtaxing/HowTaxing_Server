package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.CalculationOwnHouseHistoryDetail;
import com.xmonster.howtaxing.model.CalculationOwnHouseHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationOwnHouseHistoryDetailRepository extends JpaRepository<CalculationOwnHouseHistoryDetail, CalculationOwnHouseHistoryId> {
}
