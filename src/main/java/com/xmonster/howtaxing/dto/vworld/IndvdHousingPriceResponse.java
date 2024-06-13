package com.xmonster.howtaxing.dto.vworld;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
// 브이월드 개별주택가격속성조회 응답 결과
public class IndvdHousingPriceResponse {
    private IndvdHousingPrices indvdHousingPrices;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class IndvdHousingPrices {
        private String resultCode;
        private String resultMsg;
        private String totalCount;
        private String pageNo;
        private String numOfRows;
        private List<ApartHousingPriceResponse.Field> field;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Field {
        private String pnu;                 // 고유번호(ex:1111016700100200000)
        private String ldCode;              // 법정동코드(ex:1111010100)
        private String ldCodeNm;            // 법정동명(ex:서울특별시 종로구 청운동)
        private String regstrSeCode;        // 특수지구분코드(ex:1)
        private String regstrSeCodeNm;      // 특수지구분명(ex:일반)
        private String mnnmSlno;            // 지번(ex:1-1)
        private String bildRegstrEsntlNo;   // 건축물대장고유번호(ex:1111016700100180103)
        private String stdrYear;            // 기준연도(ex:2012)
        private String stdrMt;              // 기준월(ex:09)
        private String dongCode;            // 동코드(ex:10)
        private Double ladRegstrAr;	        // 토지대장면적(㎡)(ex:147.8)
        private Double calcPlotAr;	        // 산정대지면적(㎡)(ex:147.8)
        private Double buldAllTotAr;	    // 건물전체연면적(㎡)(ex:42.98)
        private Double buldCalcTotAr;	    // 건물산정연면적(㎡)(ex:42.98)
        private Long housePc;               // 주택가격(원)(ex:268000000)
        private String stdLandAt;           // 표준지여부(ex:Y 또는 N)
        private String lastUpdtDt;          // 데이터기준일자(ex:2016-10-05)
    }
}
