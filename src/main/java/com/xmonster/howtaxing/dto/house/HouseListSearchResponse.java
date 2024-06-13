package com.xmonster.howtaxing.dto.house;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HouseListSearchResponse {
    private int listCnt;
    private List<HouseSimpleInfoResponse> list;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class HouseSimpleInfoResponse {
        private Long houseId;                   // 주택ID
        private String houseType;               // 주택유형
        private String houseName;               // 주택명
        private String roadAddr;                // 도로명주소
        private String detailAdr;               // 상세주소
        private Boolean isRequiredDataMissing;  // 필수데이터누락여부
    }
}
