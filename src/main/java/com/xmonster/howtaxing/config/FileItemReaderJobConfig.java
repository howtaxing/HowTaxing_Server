package com.xmonster.howtaxing.config;

import com.xmonster.howtaxing.dto.house.HousePubLandPriceInfoDto;
import com.xmonster.howtaxing.model.HousePubLandPriceInfo;
import com.xmonster.howtaxing.utils.CsvReader;
import com.xmonster.howtaxing.utils.CsvWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FileItemReaderJobConfig {
    // TODO. 추후 CSV 등록이 필요한 시점에 수정 예정
    /*private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final CsvReader csvReader;
    private final CsvWriter csvWriter;

    private static final int chunkSize = 1000;

    @Bean
    public Job csvFileItemReaderJob() throws Exception{
        return jobBuilderFactory.get("csvFileItemReaderJob")
                .incrementer(new RunIdIncrementer())
                .start(csvFileItemReaderStep())
                .build();
    }

    @Bean
    public Step csvFileItemReaderStep() throws Exception{
        return stepBuilderFactory.get("csvFileItemReaderStep")
                .<HousePubLandPriceInfoDto, HousePubLandPriceInfo>chunk(chunkSize)
                .reader(csvReader.csvFileItemReader())
                .writer(csvWriter)
                .build();
    }*/
}
