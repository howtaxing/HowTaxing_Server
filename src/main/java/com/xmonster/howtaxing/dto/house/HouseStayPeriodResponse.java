package com.xmonster.howtaxing.dto.house;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HouseStayPeriodResponse {
    private Long houseId;                   // 주택 ID
    private String step;                    // 로그인 단계(1:init, 2:sign)
    private String stepData;                // 로그인 사용데이터(로그인 단계 1에 응답)
    private String houseName;               // 주택명(로그인 단계 2에 응답)
    private String detailAdr;               // 상세주소(로그인 단계 2에 응답)
    private Boolean hasStayInfo;            // 거주정보존재여부(로그인 단계 2에 응답)
    private String stayPeriodInfo;          // 거주기간정보(로그인 단계 2에 응답)
    private String stayPeriodCount;         // 거주기간일자(로그인 단계 2에 응답)
    private String stayPeriodDetailContent; // 거주기간상세내용(로그인 단계 2에 응답)
}
