package com.xmonster.howtaxing.dto.house;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HouseRegistRequest {
    private String houseType;           // 주택유형
    private String houseName;           // 주택명
    private String detailAdr;           // 상세주소
    private String jibunAddr;           // 지번주소
    private String roadAddr;            // 도로명주소
    private String roadAddrRef;         // 도로명주소참고항목
    private String bdMgtSn;             // 건물관리번호
    private String admCd;               // 행정구역코드
    private String rnMgtSn;             // 도로명코드
    private Boolean isMoveInRight;      // 입주권여부
}
