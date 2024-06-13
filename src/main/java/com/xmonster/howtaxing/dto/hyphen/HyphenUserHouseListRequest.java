package com.xmonster.howtaxing.dto.hyphen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하이픈 주택소유확인(청약홈)
 * https://www.hyphen.im/product-api/view?seq=169
 * v1.0 : 최초 작성 / 2024.03.02 김웅태
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HyphenUserHouseListRequest {
    private String loginMethod;     // (사용)로그인구분(CERT : 공동인증서, EASY : 간편로그인(네이버,kb인증서), FINCERT : 금융인증서)
    private String loginOrgCd;      // (사용)간편로그인 기관 구분(kb : KB국민, naver : 네이버, toss : 토스) *제한시간:3분 / 간편로그인시 필수
    private String signCert;        // ----------인증서정보(인증서 PEM문자열)
    private String signPri;         // ----------개인키정보(개인키 PEM문자열)
    private String signPw;          // ----------인증서비밀번호
    private String bizNo;           // (사용)주민등록번호(13자리)
    private String cloudCertYn;     // ----------금융인증서 사용 여부(Y : 금융인증서 사용) *기본값:N / 금융인증서 필수
    private String step;            // ----------로그인 단계(1.init : 금융인증서 서명 시 필요한 데이터(nonce) 요청, 2.sign : 금융인증서 서명값으로 로그인) *금융인증서 필수
    private String stepData;        // ----------로그인 사용데이터(내부용) *금융인증서 step 1 결과의 'stepData'를 그대로 입력
    private String signData;        // ----------금융인증서 서명값(hex string으로 입력) *금융인증서 필수
    private String userId;          // (사용)네이버아이디 *네이버인증서 로그인시 필수
    private String userPw;          // (사용)네이버비밀번호 *네이버인증서 로그인시 필수
    private String mobileNo;        // (사용)휴대폰번호 *KB국민인증서/토스인증서 로그인시 필수
    private String userNm;          // (사용)이름 *KB국민인증서/토스인증서 로그인
}
