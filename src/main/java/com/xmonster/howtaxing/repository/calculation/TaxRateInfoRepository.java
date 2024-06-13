package com.xmonster.howtaxing.repository.calculation;

import com.xmonster.howtaxing.model.TaxRateInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TaxRateInfoRepository extends JpaRepository<TaxRateInfo, String> {
    // 세율코드 목록 조회
    @Query(value = "SELECT tax_rate_code FROM tax_rate_info t GROUP BY tax_rate_code", nativeQuery = true)
    List<Map<String, Object>> findTaxRateCode();

    // (세율코드로) 세율명 조회
    @Query(value = "SELECT tax_rate_name FROM tax_rate_info t WHERE (t.tax_rate_code = :taxRateCode)", nativeQuery = true)
    Map<String, Object> findTaxRateNameByTaxRateCode(@Param("taxRateCode") String taxRateCode);

    // 사용함수 목록 조회
    @Query(value = "SELECT used_func FROM tax_rate_info t WHERE used_func != '' GROUP BY used_func", nativeQuery = true)
    List<Map<String, Object>> findUsedFunc();

    Optional<TaxRateInfo> findByTaxRateCode(String taxRateCode);
}
