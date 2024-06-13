package com.xmonster.howtaxing.dto.jusogov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JusoGovRoadAdrRequestDto {
    private String confmKey;
    private String currentPage;
    private String countPerPage;
    private String keyword;
    private String resultType;
    private String hstryYn;
    private String firstSort;
    private String addInfoYn;
}
