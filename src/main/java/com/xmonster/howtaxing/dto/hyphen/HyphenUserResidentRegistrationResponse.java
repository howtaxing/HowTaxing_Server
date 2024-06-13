package com.xmonster.howtaxing.dto.hyphen;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HyphenUserResidentRegistrationResponse {
    @JsonProperty("common")
    private HyphenUserResidentRegistrationCommon hyphenUserResidentRegistrationCommon;
    @JsonProperty("data")
    private HyphenUserResidentRegistrationData hyphenUserResidentRegistrationData;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class HyphenUserResidentRegistrationCommon {
        @JsonProperty("userTrNo")
        private String userTrNo;
        @JsonProperty("hyphenTrNo")
        private String hyphenTrNo;
        @JsonProperty("errYn")
        private String errYn;
        @JsonProperty("errMsg")
        private String errMsg;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class HyphenUserResidentRegistrationData {
        @JsonProperty("성명")
        private String name;
        @JsonProperty("주민등록번호")
        private String rlno;
        @JsonProperty("인적사항변경내용")
        private List<String> personalInfoChangeContents;
        @JsonProperty("인적사항변경내역")
        private List<String> personalInfoChangeHistory;
        @JsonProperty("주민번호변경내역")
        private List<String> rlnoChangeHistory;
        @JsonProperty("변동내역")
        private List<ChangeHistory> changeHistoryList;
        @JsonProperty("병역사항")
        private MilitaryService militaryService;
        @JsonProperty("stepData")
        private String stepData;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        /* 변동내역 */
        public static class ChangeHistory {
            @JsonProperty("번호")
            private String number;
            @JsonProperty("주소")
            private String address;
            @JsonProperty("발생일")
            private String occurrenceDate;
            @JsonProperty("신고일")
            private String reportDate;
            @JsonProperty("세대주_및_관계")
            private String headOfHouseHoldAndRelationShip;
            @JsonProperty("변동사유")
            private String changeReason;
            @JsonProperty("등록상태")
            private String registrationStatus;
        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        /* 병역사항 */
        public static class MilitaryService {
            @JsonProperty("역종")
            private String field01;
            @JsonProperty("군별")
            private String field02;
            @JsonProperty("군번")
            private String field03;
            @JsonProperty("입영일")
            private String field04;
            @JsonProperty("병과")
            private String field05;
            @JsonProperty("계급")
            private String field06;
            @JsonProperty("전역근거")
            private String field07;
            @JsonProperty("전역일자")
            private String field08;
            @JsonProperty("전역사유")
            private String field09;
            @JsonProperty("신체등위")
            private String field10;
            @JsonProperty("처분일자")
            private String field11;
            @JsonProperty("처분사항")
            private String field12;
        }
    }
}