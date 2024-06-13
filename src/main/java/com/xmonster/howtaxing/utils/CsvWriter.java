package com.xmonster.howtaxing.utils;

import com.xmonster.howtaxing.dto.house.HousePubLandPriceInfoDto;
import com.xmonster.howtaxing.model.HousePubLandPriceInfo;
import com.xmonster.howtaxing.repository.house.HousePubLandPriceInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CsvWriter implements ItemWriter<HousePubLandPriceInfo> {

    private final HousePubLandPriceInfoRepository housePubLandPriceInfoRepository;

    @Override
    public void write(List<? extends HousePubLandPriceInfo> list) throws Exception {
        housePubLandPriceInfoRepository.saveAll(new ArrayList<HousePubLandPriceInfo>(list));
    }
}