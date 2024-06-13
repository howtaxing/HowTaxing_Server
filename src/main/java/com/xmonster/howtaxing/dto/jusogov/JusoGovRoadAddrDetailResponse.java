package com.xmonster.howtaxing.dto.jusogov;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
// 도로명주소 상세주소 조회 응답 결과
public class JusoGovRoadAddrDetailResponse {
    private Integer totalCount;
    private String searchType;
    private List<String> dongHoList;
}