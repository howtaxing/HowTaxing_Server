package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.CalculationCommentaryResponseHistory;
import com.xmonster.howtaxing.model.CalculationHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CalculationCommentaryResponseHistoryRepository extends JpaRepository<CalculationCommentaryResponseHistory, CalculationHistoryId> {

    //List<CalculationCommentaryResponseHistory> findByCalculationHistoryId(CalculationHistoryId calculationHistoryId);

    @Query(value = "SELECT * FROM calculation_commentary_response_history c WHERE c.calc_history_id = :calcHistoryId", nativeQuery = true)
    List<CalculationCommentaryResponseHistory> findByCalcHistoryId(@Param("calcHistoryId") Long calcHistoryId);
}
