package com.xmonster.howtaxing.dto.hyphen;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HyphenUserHouseListResponse {
    @JsonProperty("common")
    private HyphenCommon hyphenCommon;
    @JsonProperty("data")
    private HyphenData hyphenData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HyphenCommon {
        @JsonProperty("userTrNo")
        private String userTrNo;
        @JsonProperty("hyphenTrNo")
        private String hyphenTrNo;
        @JsonProperty("errYn")
        private String errYn;
        @JsonProperty("errMsg")
        private String errMsg;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HyphenData {
        @JsonProperty("listMsg1")
        private String listMsg1;
        @JsonProperty("list1")
        private List<DataDetail1> list1;

        @JsonProperty("listMsg2")
        private String listMsg2;
        @JsonProperty("list2")
        private List<DataDetail2> list2;

        @JsonProperty("listMsg3")
        private String listMsg3;
        @JsonProperty("list3")
        private List<DataDetail3> list3;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        /* 건축물대장정보 */
        public static class DataDetail1 {
            private String name;                        // 성명
            private String address;                     // 주소
            private String area;                        // 면적(m^2)
            private String approvalDate;                // 사용승인일
            private String reasonChangeOwnership;       // 소유권변동사유
            private String ownershipChangeDate;         // 소유권변동일
            private String publicationBaseDate;         // 공시기준일
            private String publishedPrice;              // 공시가격(천원)
            private String baseDate;                    // 기준일자
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        /* 부동산거래내역(주택분) */
        public static class DataDetail2 {
            private String name;                        // 성명
            private String address;                     // 주소
            private String sellBuyClassification;       // 매도/매수구분
            private String area;                        // 전용면적(m^2)
            private String tradingPrice;                // 매매가
            private String balancePaymentDate;          // 잔금지급일
            private String contractDate;                // 계약일자
            private String startDate;                   // 신고분기준 시작일
            private String endDate;                     // 신고분기준 종료일
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        /* 재산세정보(주택분) */
        public static class DataDetail3 {
            private String name;                        // 성명
            private String address;                     // 주소
            private String area;                        // 전용면적(m^2)
            private String acquisitionDate;             // 취득일자
            private String baseDate;                    // 기준일자
        }
    }
}