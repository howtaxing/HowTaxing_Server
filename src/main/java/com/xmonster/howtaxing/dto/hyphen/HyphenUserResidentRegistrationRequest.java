package com.xmonster.howtaxing.dto.hyphen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하이픈 주택소유확인(청약홈)
 * https://www.hyphen.im/product-api/view?seq=69
 * v1.0 : 최초 작성 / 2024.04.20 김웅태
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HyphenUserResidentRegistrationRequest {

    private String signCert;        // [선택] 인증서정보(인증서 PEM 문자열) *인증서로그인 필수
    private String signPri;         // [선택] 개인키정보(개인키 PEM 문자열) *인증서로그인 필수
    private String signPw;          // [선택] 인증서비밀번호 *인증서로그인 signPw 또는 signPwEnc필수
    private String signPwEnc;       // [선택] (암호화)인증서비밀번호 *인증서로그인 signPw 또는 signPwEnc필수

    private String sido;            // [필수] 시도
    private String sigg;            // [필수] 시군구
    private String cusGb;           // [필수] 신청자 구분(01: 개인(내국인) 02: 법인 03: 개인(외국인))
    private String userName;        // [필수] 비회원 이름 *비회원 userName 또는 userNameEnc 필수
    private String userNameEnc;     // [선택] (암호화)비회원 이름 *비회원 userName 또는 userNameEnc 필수
    private String bizNo;           // [필수] 비회원 주민등록번호 *비회원시 bizNo 또는 bizNoEnc 필수
    private String bizNoEnc;        // [선택] (암호화)비회원 주민등록번호 *비회원시 bizNo 또는 bizNoEnc 필수

    private String req2Opt1;        // [선택] 개인인적사항변경내역(01:포함, 02:미포함)
    private String req2Opt2;        // [선택] 과거의주소변동사항(01:포함, 02:미포함)
    private String req2Opt3;        // [선택] 세대주성명관계(01:포함, 02:미포함)
    private String req2Opt4;        // [선택] 주민등록번호뒷자리(01:포함, 02:미포함)
    private String req2Opt5;        // [선택] 전입변동일(01:포함, 02:미포함)
    private String req2Opt6;        // [선택] 변동사유(01:포함, 02:미포함)
    private String req2Opt7;        // [선택] 병역사항(01:포함, 02:미포함)
    private String req2Opt8;        // [선택] 국내거소신고외국인등록번호(01:포함, 02:미포함)

    private String authMethod;      // [필수] 인증방법선택(CERT: 공동인증서 EASY: 간편인증 FINCERT: 금융인증서) *기본은 CERT
    private String loginOrgCd;      // [필수] 간편로그인 기관구분(pass : PASS인증 kakao : 카카오톡 payco : 페이코 kica : 삼성패스 kb : KB스타뱅킹) *간편인증시 필수
    private String mobileNo;        // [필수] 휴대폰번호 *간편인증시 mobileNo 또는 mobileNoEnc 필수
    private String mobileNoEnc;     // [선택] (암호화)휴대폰번호 *간편인증시 mobileNo 또는 mobileNoEnc 필수
    private String mobileCo;        // [선택] pass인증시 통신사(SKT : S KT : K LGU+ : L) *앋뜰통신사구분없음
    private String step;            // [필수] 로그인단계(간편인증시 1단계 : init, 2단계 : sign / 금융인증서 1단계 : getMinwonInfo, 2단계 : insertMinwon) *간편인증/금융인증서 필수
    private String stepData;        // [선택] 로그인 사용데이터(간편인증 : "init" 단계 결과의 stepData 그대로 입력 / 금융인증서 : "getMinwonInfo"단계 결과의 'stepData'를 그대로 입력) *간편인증/금융인증서 2단계 필수
}