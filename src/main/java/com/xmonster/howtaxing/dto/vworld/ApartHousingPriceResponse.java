package com.xmonster.howtaxing.dto.vworld;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
// 브이월드 공동주택가격속성조회 응답 결과
public class ApartHousingPriceResponse {
    private ApartHousingPrices apartHousingPrices;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ApartHousingPrices {
        private String resultCode;
        private String resultMsg;
        private String totalCount;
        private String pageNo;
        private String numOfRows;
        private List<Field> field;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Field {
        private String pnu;                 // 고유번호(ex:1144012700116340000)
        private String ldCode;              // 법정동코드(ex:1144012700)
        private String ldCodeNm;            // 법정동명(ex:서울특별시 마포구 상암동)
        private String regstrSeCode;        // 특수지구분코드(ex:1)
        private String regstrSeCodeNm;      // 특수지구분명(ex:일반)
        private String mnnmSlno;            // 지번(ex:1634)
        private String stdrYear;            // 기준연도(ex:2012)
        private String stdrMt;              // 기준월(ex:01)
        private String aphusCode;	        // 공동주택코드(ex:20022499)
        private String aphusSeCode;	        // 공동주택구분코드(ex:1)
        private String aphusSeCodeNm;	    // 공동주택구분명(ex:아파트)
        private String spclLandNm;	        // 특수지명(ex:상암택지개발사업지구2-1블럭)
        private String aphusNm;	            // 공동주택명(ex:상암월드컵1단지)
        private String dongNm;	            // 동명(ex:101)
        private String floorNm;	            // 층명(ex:2)
        private String hoNm;	            // 호명(ex:201)
        private Double prvuseAr;	        // 전용면적(㎡)(ex:39.66)
        private Long pblntfPc;	            // 공시가격(원)(ex:60000000)
        private String lastUpdtDt;          // 데이터기준일자(ex:2016-10-05)
    }
}
