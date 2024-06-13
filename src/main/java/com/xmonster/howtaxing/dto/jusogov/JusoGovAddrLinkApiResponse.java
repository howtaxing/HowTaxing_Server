package com.xmonster.howtaxing.dto.jusogov;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JusoGovAddrLinkApiResponse {

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
            private Integer currentPage;    // 페이지 번호
            private Integer countPerPage;   // 페이지당 출력할 결과 Row수
            private String errorCode;       // 에러 코드
            private String errorMessage;    // 에러 메시지
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class JusoDetail {
            private String roadAddr;        // 전체 도로명 주소
            private String roadAddrPart1;   // 도로명주소(참고항목 제외)
            private String roadAddrPart2;   // 도로명주소 참고항목
            private String jibunAddr;       // 지번주소
            private String engAddr;         // 도로명주소(영문)
            private String zipNo;           // 우편번호
            private String admCd;           // 행정구역코드
            private String rnMgtSn;         // 도로명코드
            private String bdMgtSn;         // 건물관리번호
            private String detBdNmList;     // 상세건물명
            private String bdNm;            // 건물명
            private String bdKdcd;          // 공동주택여부(1:공동주택 0:비공동주택)
            private String siNm;            // 시도명
            private String sggNm;           // 시군구명
            private String emdNm;           // 읍면동명
            private String liNm;            // 법정리명
            private String rn;              // 도로명
            private String udrtYn;          // 지하여부(0:지상 1:지하)
            private String buldMnnm;        // 건물본번
            private String buldSlno;        // 건물부번
            private String mtYn;            // 산여부(0:대지 1:산)
            private String lnbrMnnm;        // 지번본번(번지)
            private String lnbrSlno;        // 지번부번(호)
            private String emdNo;           // 읍면동일련번호
            private String hstryYn;         // 변동이력여부(0:현행 주소정보, 1:요청변수 keyword(검색어)가 변동된 주소정보에서 검색된 정보)
            private String relJibun;        // 관련지번
            private String hemdNm;          // 관할주민센터
        }
    }
}