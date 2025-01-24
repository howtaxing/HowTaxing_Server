package com.xmonster.howtaxing.utils;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.model.House;
import com.xmonster.howtaxing.repository.house.HouseRepository;
import com.xmonster.howtaxing.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HouseUtil {
    private final UserUtil userUtil;
    private final HouseRepository houseRepository;

    public House findSelectedHouse(Long houseId) {
        return houseRepository.findByHouseId(houseId)
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR));
    }

    public List<House> findOwnHouseList() {
        return houseRepository.findByUserId(userUtil.findCurrentUserId());
    }

    public long countOwnHouse() {
        return houseRepository.countByUserId(userUtil.findCurrentUserId());
    }
}
