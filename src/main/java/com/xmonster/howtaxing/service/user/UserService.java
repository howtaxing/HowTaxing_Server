package com.xmonster.howtaxing.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.sms.SmsSendMessageRequest;
import com.xmonster.howtaxing.dto.user.*;
import com.xmonster.howtaxing.feign.apple.AppleAuthApi;
import com.xmonster.howtaxing.feign.kakao.KakaoUserApi;
import com.xmonster.howtaxing.feign.naver.NaverAuthApi;
import com.xmonster.howtaxing.feign.naver.NaverUserApi;
import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.house.HouseRepository;
import com.xmonster.howtaxing.repository.user.UserRepository;
import com.xmonster.howtaxing.service.jwt.JwtService;
import com.xmonster.howtaxing.service.redis.RedisService;
import com.xmonster.howtaxing.service.sms.SmsAuthService;
import com.xmonster.howtaxing.service.sms.SmsMessageService;
import com.xmonster.howtaxing.type.*;
import com.xmonster.howtaxing.utils.GsonLocalDateTimeAdapter;
import com.xmonster.howtaxing.utils.UserUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.KeyFactory;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.LocalDateTime;
import java.util.*;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final JwtService jwtService;
    private final RedisService redisService;
    private final SmsAuthService smsAuthService;
    private final SmsMessageService smsMessageService;

    private final UserRepository userRepository;
    private final HouseRepository houseRepository;

    private final UserUtil userUtil;

    private final KakaoUserApi kakaoUserApi;
    private final NaverUserApi naverUserApi;
    private final NaverAuthApi naverAuthApi;
    private final AppleAuthApi appleAuthApi;

    private final PasswordEncoder passwordEncoder;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverAppKey;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverAppSecret;

    @Value("${jwt.access.expiration}")
    private String accessTokenExpiration;

    // 회원가입
    public Object signUp(UserSignUpDto userSignUpDto) throws Exception {
        log.info(">> [Service]UserService signUp - 회원가입");

        this.validationCheckForSignUp(userSignUpDto);

        String joinType = userSignUpDto.getJoinType();
        String id = userSignUpDto.getId();
        String password = userSignUpDto.getPassword();
        String email = userSignUpDto.getEmail();
        String phoneNumber = userSignUpDto.getPhoneNumber();
        boolean isMktAgr = userSignUpDto.getMktAgr();
        String authKey = userSignUpDto.getAuthKey();

        boolean isCheckAuthKey = smsAuthService.checkAuthKey(authKey);
        if(!isCheckAuthKey) throw new CustomException(ErrorCode.JOIN_USER_AUTH_ERROR);

        // 아이디/비밀번호 회원가입
        if(SocialType.IDPASS.toString().equals(joinType)){
            // 이미 가입된 아이디
            if(userRepository.findBySocialId(id).orElse(null) != null){
                throw new CustomException(ErrorCode.JOIN_USER_ID_EXIST);
            }

            // 이미 가입된 계정의 휴대폰 번호
            if(userUtil.findUserByPhoneNumber(phoneNumber) != null){
                throw new CustomException(ErrorCode.JOIN_DUPLICATE_PHONENUMBER_ERROR);
            }

            User createdUser = User.builder()
                    .socialId(id)
                    .socialType(SocialType.IDPASS)
                    .password(password)
                    .phoneNumber(phoneNumber)
                    .email(email)
                    .isMktAgr(isMktAgr)
                    .role(Role.USER)
                    .isLocked(false)
                    .attemptFailedCount(0)
                    .build();

            String accessToken = jwtService.createAccessToken(id);
            String refreshToken = jwtService.createRefreshToken();

            createdUser.updateRefreshToken(refreshToken);
            createdUser.passwordEncode(passwordEncoder);

            userRepository.save(createdUser);

            log.info("일반 회원가입에 성공하였습니다. 아이디 : {}", createdUser.getSocialId());
            log.info("일반 회원가입에 성공하였습니다. AccessToken : {}", accessToken);
            log.info("발급된 AccessToken 만료 기간 : {}", accessTokenExpiration);

            // 인증키 사용 완료 세팅
            smsAuthService.setAuthKeyUsed(authKey);

            return ApiResponse.success(
                    SocialLoginResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .role(createdUser.getRole())
                            .build());
        }
        // 소셜 회원가입
        else{
            // 이미 가입된 계정의 휴대폰 번호
            if(userUtil.findUserByPhoneNumber(phoneNumber) != null){
                throw new CustomException(ErrorCode.JOIN_DUPLICATE_PHONENUMBER_ERROR);
            }

            User findUser = userUtil.findUserBySocialId(id);
            findUser.authorizeUser();               // 유저 권한 세팅(GUEST -> USER)
            findUser.setPhoneNumber(phoneNumber);   // 휴대폰번호 세팅
            findUser.setMktAgr(isMktAgr);           // 마케팅동의여부 세팅

            userRepository.save(findUser);

            log.info("소셜 회원가입에 성공하였습니다. 아이디 : {}", findUser.getSocialId());

            // 인증키 사용 완료 세팅
            smsAuthService.setAuthKeyUsed(authKey);

            return ApiResponse.success(
                    SocialLoginResponse.builder()
                            .role(findUser.getRole())
                            .build());
        }
    }

    // 아이디 중복체크
    public Object idDuplicateCheck(String id) throws Exception {
        log.info(">> [Service]UserService idDuplicateCheck - 아이디 중복체크");

        if(StringUtils.isBlank(id)){
            throw new CustomException(ErrorCode.ID_CHECK_INPUT_ERROR, "아이디가 입력되지 않았습니다.");
        }

        User user = userRepository.findBySocialId(id).orElse(null);

        if(user != null){
            throw new CustomException(ErrorCode.ID_CHECK_ALREADY_EXIST);
        }

        return ApiResponse.success(Map.of("result", "사용할 수 있는 아이디 입니다."));
    }

    // 회원탈퇴
    public Object withdraw() throws Exception {
        log.info(">> [Service]UserService withdraw - 회원탈퇴");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();
        SocialType socialType = findUser.getSocialType();

        if(logoutAndUnlinkSocialAccount(TWO, findUser, socialType)){
            houseRepository.deleteByUserId(findUser.getId());   // (회원)주택 정보 삭제
            userRepository.deleteById(findUser.getId());        // (회원)사용자 정보 삭제
        }else{
            throw new CustomException(ErrorCode.USER_WITHDRAW_ERROR);
        }

        return ApiResponse.success(Map.of("result", "회원탈퇴가 완료되었습니다."));
    }

    // 로그인
    public Object login(UserLoginDto userLoginDto) throws Exception {
        log.info(">> [Service]UserService login - 로그인");

        if(userLoginDto != null){
            log.info("userLoginDto : " + userLoginDto.toString());
        }

        return ApiResponse.success(Map.of("result", "로그인이 완료되었습니다."));
    }

    // 로그아웃
    public Object logout() throws Exception {
        log.info(">> [Service]UserService logout - 로그아웃");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();
        SocialType socialType = findUser.getSocialType();

        if(logoutAndUnlinkSocialAccount(ONE, findUser, socialType)){
            // 리프레시 토큰 초기화
            findUser.setRefreshToken(EMPTY);
            userRepository.save(findUser);
        }else{
            throw new CustomException(ErrorCode.USER_LOGOUT_ERROR);
        }

        return ApiResponse.success(Map.of("result", "로그아웃이 완료되었습니다."));
    }

    // 소셜 로그인
    public Object socialLogin(SocialLoginRequest socialLoginRequest) throws Exception {
        log.info(">> [Service]UserService socialLogin - 소셜로그인");

        // 소셜로그인 유효성 검증
        this.validationCheckForSocialLogin(socialLoginRequest);

        SocialType socialType = socialLoginRequest.getSocialType();
        String socialAccessToken = socialLoginRequest.getAccessToken();
        String identityToken = socialLoginRequest.getIdentityToken();

        SocialUserResponse socialUserResponse = null;

        // 카카오 로그인
        if(SocialType.KAKAO.equals(socialType)){
            socialUserResponse = this.getKakaoUserInfo(socialAccessToken);
        }
        // 네이버 로그인
        else if(SocialType.NAVER.equals(socialType)){
            socialUserResponse = this.getNaverUserInfo(socialAccessToken);
        }
        // 애플 로그인
        else if(SocialType.APPLE.equals(socialType)){
            socialUserResponse = this.getAppleUserInfo(identityToken);
        }

        if(socialUserResponse == null) throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR, "소셜로그인 응답값이 없습니다.");

        String socialId = socialUserResponse.getId();
        if(StringUtils.isBlank(socialId)) throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR, "소셜로그인 응답값이 없습니다.");

        String accessToken = jwtService.createAccessToken(socialId);
        String refreshToken = jwtService.createRefreshToken();
        
        User user = userRepository.findBySocialId(socialId).orElse(null);
        
        // 비회원(GUEST로 회원가입 처리)
        if(user == null){
            user = User.builder()
                    .socialType(socialType)
                    .socialId(socialId)
                    .email(socialUserResponse.getEmail())
                    .name(socialUserResponse.getName())
                    .socialAccessToken(socialAccessToken)
                    .role(Role.GUEST)
                    .build();

            //userRepository.save(user);
        }else{
            user.setAttemptFailedCount(0);
            user.setIsLocked(false);
            user.updateRefreshToken(refreshToken);
            user.setSocialAccessToken(socialAccessToken);
        }

        userRepository.saveAndFlush(user);

        log.info("로그인에 성공하였습니다. 아이디 : {}", user.getSocialId());
        log.info("로그인에 성공하였습니다. AccessToken : {}", accessToken);
        log.info("사용자 Role : {}", user.getRole());
        log.info("발급된 AccessToken 만료 기간 : {}", accessTokenExpiration);

        return ApiResponse.success(
                SocialLoginResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .role(user.getRole())
                        .build());
    }

    // 아이디 찾기
    public Object findUserId(UserFindIdRequest userFindIdRequest) throws Exception {
        log.info(">> [Service]UserService findUserId - 아이디 찾기");

        this.validationCheckForFindUserId(userFindIdRequest);

        String phoneNumber = userFindIdRequest.getPhoneNumber();
        String authKey = userFindIdRequest.getAuthKey();

        String socialId = userUtil.findUserSocialIdByPhoneNumber(phoneNumber);
        if(StringUtils.isBlank(socialId)) throw new CustomException(ErrorCode.ID_FIND_INPUT_ERROR, "입력한 전화번호로 아이디를 찾지 못했어요.");

        // 인증키 검증
        boolean isCheckAuthKey = smsAuthService.checkAuthKey(authKey);
        if(!isCheckAuthKey) throw new CustomException(ErrorCode.ID_FIND_AUTH_ERROR);

        String messageContent = "회원님의 아이디는 [" + socialId + "] 입니다.";
        SmsSendMessageRequest smsSendMessageRequest =
                SmsSendMessageRequest.builder()
                        .phoneNumber(phoneNumber)
                        .messageContent(messageContent)
                        .build();

        // 아이디를 문자로 전송
        boolean isSendMessage = smsMessageService.sendMessage(smsSendMessageRequest);

        // SMS로 아이디 정보 발송 실패
        if(!isSendMessage) throw new CustomException(ErrorCode.ID_FIND_MESSAGE_ERROR);

        // 인증키 사용 완료 세팅
        smsAuthService.setAuthKeyUsed(authKey);

        return ApiResponse.success(Map.of("result", "아이디를 문자로 전송 완료했어요."));
    }

    // 비밀번호 재설정
    public Object resetPassword(UserResetPasswordRequest userResetPasswordRequest) throws Exception {
        log.info(">> [Service]UserService resetPassword - 비밀번호 재설정");

        this.validationCheckForResetPassword(userResetPasswordRequest);

        String phoneNumber = userResetPasswordRequest.getPhoneNumber();
        String id = userResetPasswordRequest.getId();
        String authKey = userResetPasswordRequest.getAuthKey();
        String newPassword = userResetPasswordRequest.getNewPassword();

        // 인증키 검증
        boolean isCheckAuthKey = smsAuthService.checkAuthKey(authKey);
        if(!isCheckAuthKey) throw new CustomException(ErrorCode.PW_RESET_AUTH_ERROR);

        User user = userUtil.findUserBySocialId(id);
        user.setPassword(newPassword);
        user.passwordEncode(passwordEncoder);

        userRepository.save(user);

        // 인증키 사용 완료 세팅
        smsAuthService.setAuthKeyUsed(authKey);

        return ApiResponse.success(Map.of("result", "비밀번호 재설정이 완료되었어요."));
    }

    // 소셜로그인 유효성 검증
    private void validationCheckForSocialLogin(SocialLoginRequest socialLoginRequest) {
        if(socialLoginRequest == null){
            throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR, "소셜로그인을 위한 입력값이 존재하지 않습니다.");
        }

        log.info("socialLoginRequest : " + socialLoginRequest);

        SocialType socialType = socialLoginRequest.getSocialType();
        String socialAccessToken = socialLoginRequest.getAccessToken();
        String socialIdentityToken = socialLoginRequest.getIdentityToken();

        if(socialType == null){
            throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR, "소셜로그인 유형이 입력되지 않았습니다.");
        }

        if(SocialType.KAKAO.equals(socialType) || SocialType.NAVER.equals(socialType)){
            if(StringUtils.isBlank(socialAccessToken)){
                throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR, "AccessToken(카카오,네이버)이 입력되지 않았습니다.");
            }
        }else if(SocialType.APPLE.equals(socialType)){
            if(StringUtils.isBlank(socialIdentityToken)){
                throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR, "IdentityToken(애플)이 입력되지 않았습니다.");
            }
        }
    }

    // 회원가입 유효성 검증
    private void validationCheckForSignUp(UserSignUpDto userSignUpDto){
        if(userSignUpDto == null){
            throw new CustomException(ErrorCode.JOIN_USER_INPUT_ERROR);
        }

        String joinType = userSignUpDto.getJoinType();
        String id = userSignUpDto.getId();
        String password = userSignUpDto.getPassword();
        String phoneNumber = userSignUpDto.getPhoneNumber();
        Boolean isMktAgr = userSignUpDto.getMktAgr();
        String authKey = userSignUpDto.getAuthKey();

        if(StringUtils.isBlank(joinType)){
            throw new CustomException(ErrorCode.JOIN_USER_INPUT_ERROR, "가입유형이 입력되지 않았습니다.");
        }else{
            if(!joinType.equals(JoinType.IDPASS.toString()) && !joinType.equals(JoinType.SOCIAL.toString())){
                throw new CustomException(ErrorCode.JOIN_USER_INPUT_ERROR, "가입유형이 올바르지 않습니다.");
            }
        }

        if(StringUtils.isBlank(id)){
            throw new CustomException(ErrorCode.JOIN_USER_INPUT_ERROR, "아이디가 입력되지 않았습니다.");
        }

        if(joinType.equals(JoinType.IDPASS.toString())){
            if(StringUtils.isBlank(password)){
                throw new CustomException(ErrorCode.JOIN_USER_INPUT_ERROR, "비밀번호가 입력되지 않았습니다.");
            }
        }

        if(StringUtils.isBlank(phoneNumber)){
            throw new CustomException(ErrorCode.JOIN_USER_INPUT_ERROR, "휴대폰번호가 입력되지 않았습니다.");
        }else{
            phoneNumber = phoneNumber.replace(HYPHEN, EMPTY);

            if(phoneNumber.length() != 11){
                throw new CustomException(ErrorCode.JOIN_USER_INPUT_ERROR, "정확한 휴대폰번호를 입력해주세요.");
            }
        }

        if(isMktAgr == null){
            throw new CustomException(ErrorCode.JOIN_USER_INPUT_ERROR, "마케팅 동의여부 값이 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(authKey)){
            throw new CustomException(ErrorCode.PW_RESET_AUTH_ERROR, "인증키가 입력되지 않았습니다.");
        }else{
            if(authKey.length() != 30){
                throw new CustomException(ErrorCode.PW_RESET_AUTH_ERROR, "정확한 인증키를 입력해주세요.");
            }
        }
    }

    // 아이디 찾기 유효성 검증
    private void validationCheckForFindUserId(UserFindIdRequest userFindIdRequest) {
        if(userFindIdRequest == null){
            throw new CustomException(ErrorCode.ID_FIND_INPUT_ERROR);
        }

        log.info(userFindIdRequest.toString());

        String phoneNumber = userFindIdRequest.getPhoneNumber();
        String authKey = userFindIdRequest.getAuthKey();

        if(StringUtils.isBlank(phoneNumber)){
            throw new CustomException(ErrorCode.ID_FIND_INPUT_ERROR, "휴대폰번호가 입력되지 않았습니다.");
        }else{
            phoneNumber = phoneNumber.replace(HYPHEN, EMPTY);

            if(phoneNumber.length() != 11){
                throw new CustomException(ErrorCode.ID_FIND_INPUT_ERROR, "정확한 휴대폰번호를 입력해주세요.");
            }
        }
        
        if(StringUtils.isBlank(authKey)){
            throw new CustomException(ErrorCode.ID_FIND_AUTH_ERROR, "인증키가 입력되지 않았습니다.");
        }else{
            if(authKey.length() != 30){
                throw new CustomException(ErrorCode.ID_FIND_AUTH_ERROR, "정확한 인증키를 입력해주세요.");
            }
        }
    }

    // 비밀번호 재설정 유효성 검증
    private void validationCheckForResetPassword(UserResetPasswordRequest userResetPasswordRequest) {
        if(userResetPasswordRequest == null){
            throw new CustomException(ErrorCode.PW_RESET_INPUT_ERROR);
        }

        log.info(userResetPasswordRequest.toString());

        String phoneNumber = userResetPasswordRequest.getPhoneNumber();
        String id = userResetPasswordRequest.getId();
        String authKey = userResetPasswordRequest.getAuthKey();
        String newPassword = userResetPasswordRequest.getNewPassword();
        String newPasswordConfirm = userResetPasswordRequest.getNewPasswordConfirm();

        String socialId = userUtil.findUserSocialIdByPhoneNumber(phoneNumber);

        if(StringUtils.isBlank(phoneNumber)){
            throw new CustomException(ErrorCode.PW_RESET_INPUT_ERROR, "휴대폰번호가 입력되지 않았습니다.");
        }else{
            phoneNumber = phoneNumber.replace(HYPHEN, EMPTY);

            if(phoneNumber.length() != 11){
                throw new CustomException(ErrorCode.PW_RESET_INPUT_ERROR, "정확한 휴대폰번호를 입력해주세요.");
            }
        }
        
        if(StringUtils.isBlank(id)){
            throw new CustomException(ErrorCode.PW_RESET_INPUT_ERROR, "아이디가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(authKey)){
            throw new CustomException(ErrorCode.PW_RESET_AUTH_ERROR, "인증키가 입력되지 않았습니다.");
        }else{
            if(authKey.length() != 30){
                throw new CustomException(ErrorCode.PW_RESET_AUTH_ERROR, "정확한 인증키를 입력해주세요.");
            }
        }
        
        if(StringUtils.isBlank(newPassword)){
            throw new CustomException(ErrorCode.PW_RESET_INPUT_ERROR, "새 비밀번호가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(newPasswordConfirm)){
            throw new CustomException(ErrorCode.PW_RESET_INPUT_ERROR, "새 비밀번호 확인 값이 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(socialId)){
            throw new CustomException(ErrorCode.PW_RESET_INPUT_ERROR, "입력한 휴대폰번호로 아이디를 찾지 못했어요.");
        }

        if(!id.equals(socialId)){
            throw new CustomException(ErrorCode.PW_RESET_INPUT_ERROR, "해당 휴대폰번호로 가입된 아이디가 아니에요.");
        }

        if(!newPassword.equals(newPasswordConfirm)){
            throw new CustomException(ErrorCode.PW_RESET_INPUT_ERROR, "새 비밀번호와 새 비밀번호 확인 값이 일치하지 않아요.");
        }
    }

    private SocialUserResponse getKakaoUserInfo(String accessToken) {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("authorization", " Bearer " + accessToken);

        ResponseEntity<?> response = kakaoUserApi.getUserInfo(headerMap);

        log.info("kakao user response");
        log.info(response.toString());

        String jsonString = response.getBody().toString();

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter())
                .create();

        KaKaoLoginResponse kaKaoLoginResponse = gson.fromJson(jsonString, KaKaoLoginResponse.class);
        KaKaoLoginResponse.KakaoLoginData kakaoLoginData = Optional.ofNullable(kaKaoLoginResponse.getKakao_account())
                .orElse(KaKaoLoginResponse.KakaoLoginData.builder().build());

        String name = Optional.ofNullable(kakaoLoginData.getProfile())
                .orElse(KaKaoLoginResponse.KakaoLoginData.KakaoProfile.builder().build())
                .getNickname();

        return SocialUserResponse.builder()
                .id(kaKaoLoginResponse.getId())
                .gender(kakaoLoginData.getGender())
                .name(name)
                .email(kakaoLoginData.getEmail())
                .build();
    }

    private SocialUserResponse getNaverUserInfo(String accessToken) {
        Map<String ,String> headerMap = new HashMap<>();
        headerMap.put("authorization", "Bearer " + accessToken);

        ResponseEntity<?> response = naverUserApi.getUserInfo(headerMap);

        log.info("naver user response");
        log.info(response.toString());

        String jsonString = response.getBody().toString();

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new GsonLocalDateTimeAdapter())
                .create();

        NaverLoginResponse naverLoginResponse = gson.fromJson(jsonString, NaverLoginResponse.class);
        NaverLoginResponse.Response naverUserInfo = naverLoginResponse.getResponse();

        return SocialUserResponse.builder()
                .id(naverUserInfo.getId())
                .gender(naverUserInfo.getGender())
                .name(naverUserInfo.getName())
                .email(naverUserInfo.getEmail())
                .build();
    }

    private SocialUserResponse getAppleUserInfo(String identityToken) throws Exception {
        Claims claims = this.verifyAppleIdentityToken(identityToken);
        String appleUserId = claims.getSubject();

        return SocialUserResponse.builder()
                .id(appleUserId)
                .build();
    }

    private Claims verifyAppleIdentityToken(String identityToken) throws Exception {
        // Apple 공개 키 가져오기
        Map<String, Object> response = appleAuthApi.getApplePublicKey();
        List<Map<String, String>> keyArray = (List<Map<String, String>>) response.get("keys");

        // identityToken의 헤더에서 kid 추출
        String tokenKid = extractKidFromToken(identityToken);

        // 적합한 공개 키 찾기
        for (Map<String, String> keyData : keyArray) {
            String kid = keyData.get("kid");

            if (kid.equals(tokenKid)) {
                PublicKey publicKey = getPublicKey(keyData);
                return Jwts.parserBuilder()
                        .setSigningKey(publicKey)
                        .build()
                        .parseClaimsJws(identityToken)
                        .getBody();
            }
        }

        throw new IllegalArgumentException("Invalid identity token");
    }

    private String extractKidFromToken(String identityToken) {
        JwsHeader<?> header = Jwts.parserBuilder()
                .build()
                .parseClaimsJws(identityToken)
                .getHeader();

        return (String) header.get("kid");
    }

    private PublicKey getPublicKey(Map<String, String> keyData) throws Exception {
        // Apple의 공개 키 데이터에서 n, e 값을 디코딩
        byte[] nBytes = Base64.getUrlDecoder().decode(keyData.get("n"));
        byte[] eBytes = Base64.getUrlDecoder().decode(keyData.get("e"));

        // BigInteger로 변환
        BigInteger modulus = new BigInteger(1, nBytes); // n
        BigInteger exponent = new BigInteger(1, eBytes); // e

        // RSA 공개 키 생성
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    // 사용자 소셜계정 로그아웃 및 회원탈퇴 처리
    private boolean logoutAndUnlinkSocialAccount(String requestType, User findUser, SocialType socialType){
        ResponseEntity<?> response = null;
        String jsonString = EMPTY;

        String socialId = StringUtils.defaultString(findUser.getSocialId());
        String socialAccessToken = StringUtils.defaultString(findUser.getSocialAccessToken());

        log.info("[로그아웃 및 회원탈퇴] socialId : " + socialId + ", socialAccessToken : " + socialAccessToken);

        boolean resultFlag = false;

        try {
            Map<String, String> headerMap = new HashMap<>();
            headerMap.put("authorization", "Bearer " + socialAccessToken);

            // 로그아웃
            if(ONE.equals(requestType)){
                if(SocialType.IDPASS.equals(socialType)){
                    log.info("일반로그인 계정 로그아웃");
                }else if(SocialType.KAKAO.equals(socialType)){
                    log.info("소셜로그인(카카오) 계정 로그아웃");
                    response = kakaoUserApi.logoutUserInfo(
                            headerMap,
                            SocialLogoutAndUnlinkRequest.builder()
                                    .targetIdType("user_id")
                                    .targetId(Long.parseLong(socialId))
                                    .build());
                }else if(SocialType.NAVER.equals(socialType)){
                    log.info("소셜로그인(네이버) 계정 로그아웃");
                    log.info("네이버는 로그아웃 기능이 없습니다.");
                }else{
                    // TODO : Apple 로그아웃 구현
                    // google, apple..
                    throw new CustomException(ErrorCode.USER_LOGOUT_ERROR, "Google과 Apple의 로그아웃 기능은 준비 중입니다.");
                }
            }
            // 회원탈퇴
            else if(TWO.equals(requestType)){
                if(SocialType.IDPASS.equals(socialType)){
                    log.info("일반로그인 계정 회원탈퇴");
                }else if(SocialType.KAKAO.equals(socialType)){
                    log.info("소셜로그인(카카오) 계정 회원탈퇴");
                    response = kakaoUserApi.unlinkUserInfo(
                            headerMap,
                            SocialLogoutAndUnlinkRequest.builder()
                                    .targetIdType("user_id")
                                    .targetId(Long.parseLong(socialId))
                                    .build());
                }else if(SocialType.NAVER.equals(socialType)){
                    log.info("소셜로그인(네이버) 계정 회원탈퇴");
                    // 네이버 회원탈퇴는 getAccessToken과 동일(grantType만 delete로 세팅)
                    response = naverAuthApi.getAccessToken("delete", naverAppKey, naverAppSecret, null, null, socialAccessToken);
                }else{
                    // google, apple..
                    throw new CustomException(ErrorCode.USER_WITHDRAW_ERROR, "Google과 Apple의 회원탈퇴 기능은 준비 중입니다.");
                }
            }
            // 그 외(오류)
            else{
                throw new CustomException(ErrorCode.ETC_ERROR, "허용되지 않은 RequestType 입니다.");
            }
        }catch(Exception e){
            if(ONE.equals(requestType)){
                log.error("로그아웃 처리중 오류 발생 : " + e.getMessage());
                throw new CustomException(ErrorCode.USER_LOGOUT_ERROR);
            }else{
                log.error("회원탈퇴 처리중 오류 발생 : " + e.getMessage());
                throw new CustomException(ErrorCode.USER_WITHDRAW_ERROR);
            }
        }

        SocialLogoutAndUnlinkResponse socialLogoutAndUnlinkResponse = null;

        if(response != null && response.getBody() != null){
            jsonString = response.getBody().toString();
            log.info(response.getBody().toString());

            socialLogoutAndUnlinkResponse = (SocialLogoutAndUnlinkResponse) convertJsonToData(jsonString);
        }

        if(SocialType.IDPASS.equals(socialType)){
            resultFlag = true;
        }else if(SocialType.KAKAO.equals(socialType)){
            if(socialLogoutAndUnlinkResponse != null){
                if(socialLogoutAndUnlinkResponse.getId().equals(Long.parseLong(socialId))){
                    resultFlag = true;
                }
            }
        }else if(SocialType.NAVER.equals(socialType)){
            if(ONE.equals(requestType)){
                resultFlag = true;
            }else if(TWO.equals(requestType)){
                if(socialLogoutAndUnlinkResponse != null){
                    if(SUCCESS.equals(socialLogoutAndUnlinkResponse.getResult())){
                        resultFlag = true;
                    }
                }
            }
        }

        return resultFlag;
    }

    // 사용자정보 세션저장
    public void saveUserSession(String accessToken) {
        User findUser = userUtil.findCurrentUser();

        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("email", findUser.getEmail());
        userInfo.put("socialId", findUser.getSocialId());
        userInfo.put("accessToken", accessToken);
        userInfo.put("refreshToken", findUser.getRefreshToken());

        redisService.saveHashMap(findUser.getId(), "userInfo", userInfo);
    }

    private Object convertJsonToData(String jsonString) {
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonString, SocialLogoutAndUnlinkResponse.class);
        }catch(Exception e){
            log.error(e.getMessage());
            throw new CustomException(ErrorCode.ETC_ERROR);
        }
    }
}