package com.xmonster.howtaxing.dto.house;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class HouseListSearchRequest {
    private String certOrg;         // 인증기관(kb:KB, naver:네이버, toss:토스, kakao:카카오)
    private String userNm;          // 이름(KB,토스,카카오)
    private String mobileNo;        // 휴대폰번호(KB,토스, 카카오)
    private String rlno;            // 주민등록번호(KB,네이버,토스,카카오)
    private String userId;          // 아이디(네이버)
    private String userPw;          // 비밀번호(네이버)
    private String calcType;        // 계산유형(01:취득세, 02:양도소득세)
    private Boolean isDummy;        // 더미여부
}
