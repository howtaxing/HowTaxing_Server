package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.DeductionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DeductionInfoRepository extends JpaRepository<DeductionInfo, String> {
    // 공제코드 조회
    @Query(value = "SELECT ded_code FROM deduction_info d GROUP BY ded_code", nativeQuery = true)
    List<Map<String, Object>> findDedCode();

    // (공제코드로) 공제내용 조회
    @Query(value = "SELECT ded_content FROM deduction_info d WHERE (d.ded_code = :dedCode)", nativeQuery = true)
    Map<String, Object> findDedContentByDedCode(@Param("dedCode") String dedCode);
    
    // 단위 조회
    @Query(value = "SELECT unit FROM deduction_info d GROUP BY unit", nativeQuery = true)
    List<Map<String, Object>> findUnit();

    Optional<DeductionInfo> findByDedCode(String dedCode);
}
