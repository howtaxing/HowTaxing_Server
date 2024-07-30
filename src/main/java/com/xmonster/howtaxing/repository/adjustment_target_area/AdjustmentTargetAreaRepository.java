package com.xmonster.howtaxing.repository.adjustment_target_area;

import com.xmonster.howtaxing.model.AdjustmentTargetAreaInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdjustmentTargetAreaRepository extends JpaRepository<AdjustmentTargetAreaInfo, Long> {

    List<AdjustmentTargetAreaInfo> findByTargetAreaStartingWith(String targetArea);
}
