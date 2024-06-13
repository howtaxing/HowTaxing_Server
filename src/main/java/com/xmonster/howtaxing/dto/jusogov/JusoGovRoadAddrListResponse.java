package com.xmonster.howtaxing.dto.jusogov;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
// 도로명주소 리스트 조회 응답 결과
public class JusoGovRoadAddrListResponse {
    private Integer totalCount;
    private Integer currentPage;
    private Integer countPerPage;
    private List<Juso> jusoList;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Juso {
        private String roadAddr;        // 도로명 주소
        private String roadAddrPart1;   // 도로명주소(참고항목 제외)
        private String roadAddrPart2;   // 도로명주소 참고항목
        private String jibunAddr;       // 지번주소
        private String admCd;           // 행정구역코드
        private String rnMgtSn;         // 도로명코드
        private String bdMgtSn;         // 건물관리번호
        private String detBdNmList;     // 상세건물명
        private String bdNm;            // 건물명
        private String bdKdcd;          // 공동주택여부(1:공동주택 0:비공동주택)
        private String udrtYn;          // 지하여부(0:지상 1:지하)
        private String buldMnnm;        // 건물본번
        private String buldSlno;        // 건물부번
        private String pnu;             // (직접생성)고유번호(법정동코드 + 산여부(1:대지, 2:산) + 지번(본번4자리+부번4자리) - 19자리)
    }
}