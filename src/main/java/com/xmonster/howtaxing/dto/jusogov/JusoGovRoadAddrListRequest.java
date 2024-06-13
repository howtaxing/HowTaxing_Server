package com.xmonster.howtaxing.dto.jusogov;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JusoGovRoadAddrListRequest {
    private Integer currentPage;    // [선택] 현재 페이지 번호
    private Integer countPerPage;   // [선택] 페이지 당 출력할 결과 row 수
    private String sido;            // [선택] 시도
    private String sigungu;         // [선택] 시군구
    private String keyword;         // [필수] 주소 검색어
}
