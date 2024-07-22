package com.xmonster.howtaxing.dto.calculation;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class CalculationBuyResultResponse {
    private int listCnt;                            // 취득세 계산결과 리스트 수
    private List<CalculationBuyOneResult> list;     // 취득세 계산결과 리스트
    private int commentaryListCnt;                  // (계산결과)해설 리스트 수
    private List<String> commentaryList;            // (계산결과)해설 리스트
    private String calculationResultTextData;       // 계산결과 텍스트 데이터

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @Builder
    public static class CalculationBuyOneResult {
        private String buyPrice;            // 취득가액
        private String buyTaxRate;          // 취득세율
        private String buyTaxPrice;         // 취득세액
        private String eduTaxRate;          // 지방교육세율
        private String eduTaxPrice;         // 지방교육세액
        private String eduDiscountPrice;    // 지방교육세감면액
        private String agrTaxRate;          // 농어촌특별세율
        private String agrTaxPrice;         // 농어촌특별세액
        private String totalTaxPrice;       // 총납부세액
    }
}
