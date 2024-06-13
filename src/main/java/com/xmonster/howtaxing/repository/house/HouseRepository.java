package com.xmonster.howtaxing.repository.house;

import com.xmonster.howtaxing.model.House;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HouseRepository extends JpaRepository<House, Long> {

    Optional<House> findByHouseId(Long houseId);

    Optional<List<House>> findByUserId(Long userId);

    void deleteByHouseId(Long houseId);

    void deleteByUserId(Long userId);

    void deleteByUserIdAndSourceType(Long userId, String sourceType);

    Long countByUserId(Long userId);
}
