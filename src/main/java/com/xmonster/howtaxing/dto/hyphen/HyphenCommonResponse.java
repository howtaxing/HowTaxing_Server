package com.xmonster.howtaxing.dto.hyphen;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HyphenCommonResponse {
    @JsonProperty("common")
    private HyphenCommon hyphenCommon;

    @JsonProperty("data")
    private Object hyphenData;

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
        @JsonProperty("errCd")
        private String errCd;
        @JsonProperty("errMsg")
        private String errMsg;
    }
}