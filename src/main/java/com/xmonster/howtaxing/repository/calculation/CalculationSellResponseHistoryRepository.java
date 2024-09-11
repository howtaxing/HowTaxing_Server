package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.CalculationHistoryId;
import com.xmonster.howtaxing.model.CalculationSellResponseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CalculationSellResponseHistoryRepository extends JpaRepository<CalculationSellResponseHistory, CalculationHistoryId> {

    //List<CalculationSellResponseHistory> findByCalculationHistoryId(CalculationHistoryId calculationHistoryId);

    @Query(value = "SELECT * FROM calculation_sell_response_history c WHERE c.calc_history_id = :calcHistoryId", nativeQuery = true)
    List<CalculationSellResponseHistory> findByCalcHistoryId(@Param("calcHistoryId") Long calcHistoryId);
}
