package com.xmonster.howtaxing.dto.vworld;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VworldPubLandPriceAndAreaRequest {
    private String bdKdcd;              // [필수] 공동주택여부(1:공동주택 0:비공동주택)
    private String pnu;                 // [필수] 고유번호(8자리 이상)
    private String dongNm;              // [선택] 동명
    private String hoNm;                // [선택] 호명
    private String detailAdr;           // [선택] 상세주소
    private Integer numOfRows;          // [선택] 검색건수(최대 1000)
    private Integer pageNo;             // [선택] 페이지 번호
}