package com.xmonster.howtaxing.dto.jusogov;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JusoGovRoadAddrDetailRequest {
    private String admCd;       // [필수] 행정구역코드
    private String rnMgtSn;     // [필수] 도로명코드
    private String udrtYn;      // [필수] 지하여부(0:지상, 1:지하)
    private String buldMnnm;    // [필수] 건물본번
    private String buldSlno;    // [필수] 건물부번
    private String searchType;  // [선택] 검색유형(1:동, 2:호)
    private String dongNm;      // [선택] 동명(검색유형이 1인 경우 세팅)
}
