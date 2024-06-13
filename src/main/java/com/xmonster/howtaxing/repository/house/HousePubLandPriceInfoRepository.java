package com.xmonster.howtaxing.repository.house;

import com.xmonster.howtaxing.model.HousePubLandPriceInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HousePubLandPriceInfoRepository extends JpaRepository<HousePubLandPriceInfo, Long> {

}
