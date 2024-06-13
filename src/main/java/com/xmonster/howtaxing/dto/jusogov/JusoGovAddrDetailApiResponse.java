package com.xmonster.howtaxing.dto.jusogov;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JusoGovAddrDetailApiResponse {

    private Results results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Results {
        private Common common;
        private List<JusoDetail> juso;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Common {
            private String totalCount;      // 총 검색 데이터 수
            private String errorCode;       // 에러 코드
            private String errorMessage;    // 에러 메시지
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class JusoDetail {
            private String admCd;           // 행정구역코드
            private String rnMgtSn;         // 도로명코드
            private String udrtYn;          // 지하여부(0:지상 1:지하)
            private String buldMnnm;        // 건물본번
            private String buldSlno;        // 건물부번
            private String bdMgtSn;         // 건물관리번호
            private String dongNm;          // 동 정보
            private String floorNm;         // 층 정보
            private String hoNm;            // 호 정보
        }
    }
}