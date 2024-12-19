package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.CalculationOwnHouseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalculationOwnHouseHistoryRepository extends JpaRepository<CalculationOwnHouseHistory, Long> {

    List<CalculationOwnHouseHistory> findByCalcHistoryId(Long calcHistoryId);
}
