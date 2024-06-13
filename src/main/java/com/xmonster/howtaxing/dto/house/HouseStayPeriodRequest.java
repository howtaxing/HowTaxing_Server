package com.xmonster.howtaxing.dto.house;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HouseStayPeriodRequest {
    private Long houseId;           // [필수] (양도)주택 ID
    private String userName;        // [필수] 이름
    private String mobileNo;        // [필수] 휴대폰번호
    private String rlno;            // [필수] 주민등록번호
    private String loginOrgCd;      // [필수] 간편로그인 기관구분(PASS인증:pass, 카카오톡:kakao, 페이코:payco, 삼성패스:kica, KB스타뱅킹:kb)
    private String mobileCo;        // [선택] 통신사(SKT:S, KT:K, LGU+:L *앋뜰통신사구분없음)(PASS는 필수)
    private String sido;            // [필수] 시도
    private String sigungu;         // [필수] 시군구
    private String step;            // [필수] 로그인단계(init:1, sign:2)
    private String stepData;        // [선택] 로그인 사용데이터(간편인증 : "init" 단계 결과의 stepData 그대로 입력)(2단계 필수)
}
