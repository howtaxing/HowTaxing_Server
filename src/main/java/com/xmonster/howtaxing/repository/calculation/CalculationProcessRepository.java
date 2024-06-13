package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.CalculationProcess;
import com.xmonster.howtaxing.model.CalculationProcessId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CalculationProcessRepository extends JpaRepository<CalculationProcess, CalculationProcessId> {
    // 계산구분 및 계산명 조회
    @Query(value = "SELECT calc_type, calc_name FROM calculation_process c GROUP BY calc_type, calc_name", nativeQuery = true)
    List<Map<String, Object>> findCalcTypeAndCalcName();

    // (계산구분으로) 분기번호 조회
    @Query(value = "SELECT branch_no FROM calculation_process c WHERE (c.calc_type = :calcType) GROUP BY branch_no", nativeQuery = true)
    List<Map<String, Object>> findBranchNo(@Param("calcType") String calcType);

    // (계산구분 및 분기번호로) 분기명 조회
    @Query(value = "SELECT branch_name FROM calculation_process c WHERE (c.calc_type = :calcType AND c.branch_no = :branchNo) LIMIT 1", nativeQuery = true)
    Map<String, Object> findBranchNameByBranchNo(@Param("calcType") String calcType, @Param("branchNo") String branchNo);

    // (계산구분 및 분기번호로) 선택번호 조회
    @Query(value = "SELECT select_no FROM calculation_process c WHERE (c.calc_type = :calcType AND c.branch_no = :branchNo) GROUP BY select_no", nativeQuery = true)
    List<Map<String, Object>> findSelectNo(@Param("calcType") String calcType, @Param("branchNo") String branchNo);

    // (계산구분 및 분기번호 및 선택번호로) 선택내용 조회
    @Query(value = "SELECT select_content FROM calculation_process c WHERE (c.calc_type = :calcType AND c.branch_no = :branchNo AND c.select_no = :selectNo) LIMIT 1", nativeQuery = true)
    Map<String, Object> findSelectContentBySelectNo(@Param("calcType") String calcType, @Param("branchNo") String branchNo, @Param("selectNo") Integer selectNo);

    Optional<CalculationProcess> findByCalculationProcessId(CalculationProcessId calculationProcessId);

    @Query(value = "SELECT * FROM calculation_process c WHERE (c.calc_type = :calcType AND c.branch_no = :branchNo)", nativeQuery = true)
    Optional<List<CalculationProcess>> findByCalcTypeAndBranchNo(@Param("calcType") String calcType, @Param("branchNo") String branchNo);
}