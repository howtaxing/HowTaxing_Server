package com.xmonster.howtaxing.repository.house;

import com.xmonster.howtaxing.model.HousePropertyTaxInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HousePropertyTaxInfoRepository extends JpaRepository<HousePropertyTaxInfo, Long> {
}
