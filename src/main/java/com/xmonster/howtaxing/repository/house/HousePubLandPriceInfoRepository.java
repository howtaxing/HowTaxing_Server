package com.xmonster.howtaxing.repository.house;

import com.xmonster.howtaxing.model.HousePubLandPriceInfo;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HousePubLandPriceInfoRepository extends JpaRepository<HousePubLandPriceInfo, Long> {
    @Query("SELECT p FROM HousePubLandPriceInfo p WHERE " +
           "(:legalDstCode IS NULL OR p.legalDstCode = :legalDstCode) AND " +
           "(:roadAddr IS NULL OR p.roadAddr = :roadAddr) AND " +
           "(:dongName IS NULL OR p.dongName LIKE %:dongName%) AND " +
           "(:hoName IS NULL OR p.hoName = :hoName)")
    List<HousePubLandPriceInfo> findByConditions(
        @Param("legalDstCode") String legalDstCode,
        @Param("roadAddr") String roadAddr,
        @Param("dongName") String dongName,
        @Param("hoName") String hoName
    );
}
