package com.xmonster.howtaxing.dto.hyphen;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HyphenUserSessionResponse {
    private Building building;                      // 건축물대장정보
    private Property property;                      // 재산세정보

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    /* 건축물대장정보 */
    public static class Building {
        private String name;                        // 건물명
        private String address;                     // 건물주소
        private String area;                        // 건물면적
        private String approvalDate;                // 건물승인일자
        private String reasonChangeOwnership;       // 소유권변경사유
        private String ownershipChangeDate;         // 소유권변경일자
        private String publicationBaseDate;         // 공시기준일자
        private String publishedPrice;              // 공시가격
        private String baseDate;                    // 데이터기준일
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    /* 재산세정보 */
    public static class Property {
        private String name;                        // 재산명
        private String address;                     // 재산주소
        private String area;                        // 재산면적
        private String acquisitionDate;             // 취득일자
        private String baseDate;                    // 데이터기준일
    }
}
