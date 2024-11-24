package com.xmonster.howtaxing.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.user.*;
import com.xmonster.howtaxing.feign.kakao.KakaoUserApi;
import com.xmonster.howtaxing.feign.naver.NaverAuthApi;
import com.xmonster.howtaxing.feign.naver.NaverUserApi;
import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.house.HouseRepository;
import com.xmonster.howtaxing.repository.user.UserRepository;
import com.xmonster.howtaxing.service.jwt.JwtService;
import com.xmonster.howtaxing.service.redis.RedisService;
import com.xmonster.howtaxing.type.ErrorCode;
import com.xmonster.howtaxing.type.Role;
import com.xmonster.howtaxing.type.SocialType;
import com.xmonster.howtaxing.utils.GsonLocalDateTimeAdapter;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final HouseRepository houseRepository;
    private final UserUtil userUtil;
    private final KakaoUserApi kakaoUserApi;
    private final NaverUserApi naverUserApi;
    private final NaverAuthApi naverAuthApi;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;

    //private final KakaoLoginServiceImpl kakaoLoginService;
    //private final NaverLoginServiceImpl naverLoginService;
    private final JwtService jwtService;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverAppKey;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverAppSecret;

    @Value("${jwt.access.expiration}")
    private String accessTokenExpiration;

    // 회원가입
    public Object signUp(UserSignUpDto userSignUpDto) throws Exception {
        log.info(">> [Service]UserService signUp - 회원가입");

        Map<String, Object> resultMap = new HashMap<>();
        String joinType = EMPTY;
        String id = EMPTY;
        String password = EMPTY;
        String email = EMPTY;
        boolean isMktAgr = false;
        User findUser = null;

        if(userSignUpDto != null){
            joinType = StringUtils.defaultString(userSignUpDto.getJoinType());
            id = StringUtils.defaultString(userSignUpDto.getId());
            password = StringUtils.defaultString(userSignUpDto.getPassword());
            email = StringUtils.defaultString(userSignUpDto.getEmail());
            isMktAgr = (userSignUpDto.getMktAgr() != null) ? userSignUpDto.getMktAgr() : false;
        }

        // 아이디/비밀번호 회원가입
        if(SocialType.IDPASS.toString().equals(joinType)){
            if(EMPTY.equals(id)){
                throw new CustomException(ErrorCode.JOIN_USER_INPUT_ERROR, "아이디가 입력되지 않았습니다.");
            }

            if(EMPTY.equals(password)){
                throw new CustomException(ErrorCode.JOIN_USER_INPUT_ERROR, "비밀번호가 입력되지 않았습니다.");
            }

            User user = userRepository.findBySocialId(id).orElse(null);
            if(user != null){
                throw new CustomException(ErrorCode.JOIN_USER_ID_EXIST);
            }

            User createdUser = User.builder()
                    .socialId(id)
                    .socialType(SocialType.IDPASS)
                    .password(password)
                    .email(email)
                    .isMktAgr(isMktAgr)
                    .role(Role.USER)
                    .isLocked(false)
                    .attemptFailedCount(0)
                    .build();

            createdUser.passwordEncode(passwordEncoder);

            findUser = userRepository.save(createdUser);
        }
        // 소셜 회원가입
        else{
            findUser = userUtil.findCurrentUser();

            findUser.authorizeUser(); // 유저 권한 세팅(GUEST -> USER)
            findUser.setMktAgr(isMktAgr); // 마케팅동의여부 세팅
        }

        resultMap.put("role", findUser.getRole());

        return ApiResponse.success(resultMap);
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

    public Object socialLogin(SocialLoginRequest socialLoginRequest) throws Exception {
        log.info(">> [Service]UserService socialLogin - 소셜로그인");

        if(socialLoginRequest == null) throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR, "소셜로그인을 위한 입력값이 존재하지 않습니다.");
        if(socialLoginRequest.getSocialType() == null) throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR, "소셜로그인 유형이 입력되지 않았습니다.");
        if(socialLoginRequest.getAccessToken() == null) throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR, "엑세스 토큰이 입력되지 않았습니다.");

        SocialUserResponse socialUserResponse = null;

        // 카카오 로그인
        if(SocialType.KAKAO.equals(socialLoginRequest.getSocialType())){
            //socialUserResponse = kakaoLoginService.getUserInfo(socialLoginRequest.getAccessToken());
            socialUserResponse = this.getKakaoUserInfo(socialLoginRequest.getAccessToken());
        }
        // 네이버 로그인
        else if(SocialType.NAVER.equals(socialLoginRequest.getSocialType())){
            //socialUserResponse = naverLoginService.getUserInfo(socialLoginRequest.getAccessToken());
            socialUserResponse = this.getNaverUserInfo(socialLoginRequest.getAccessToken());
        }

        if(socialUserResponse == null) throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR, "소셜로그인 응답값이 없습니다.");

        String socialId = socialUserResponse.getId();
        if(StringUtils.isBlank(socialId)) throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR, "소셜로그인 응답값이 없습니다.");
        
        User user = userRepository.findBySocialId(socialId).orElse(null);
        
        // 비회원(GUEST로 회원가입 처리)
        if(user == null){
            user = User.builder()
                    .socialType(socialLoginRequest.getSocialType())
                    .socialId(socialId)
                    .email(socialUserResponse.getEmail())
                    .name(socialUserResponse.getName())
                    .socialAccessToken(socialLoginRequest.getAccessToken())
                    .role(Role.GUEST)
                    .build();

            userRepository.save(user);
        }

        String accessToken = jwtService.createAccessToken(socialId);
        String refreshToken = jwtService.createRefreshToken();

        user.setAttemptFailedCount(0);
        user.setIsLocked(false);
        user.updateRefreshToken(refreshToken);

        userRepository.save(user);

        //response.addHeader(jwtService.getAccessHeader(), "Bearer " + accessToken);
        //response.addHeader(jwtService.getRefreshHeader(), "Bearer " + refreshToken);

        //jwtService.sendAccessAndRefreshToken(response, accessToken, refreshToken);
        //jwtService.updateRefreshToken(user.getSocialId(), refreshToken);

        //user.updateRefreshToken(refreshToken);
        //userRepository.saveAndFlush(user);

        log.info("로그인에 성공하였습니다. 아이디 : {}", user.getSocialId());
        log.info("로그인에 성공하였습니다. AccessToken : {}", accessToken);
        log.info("발급된 AccessToken 만료 기간 : {}", accessTokenExpiration);

        return ApiResponse.success(
                SocialLoginResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .role(user.getRole())
                        .build());

        //return ApiResponse.success(Map.of("result", "로그인이 완료되었습니다."));
    }

    public SocialUserResponse getKakaoUserInfo(String accessToken) {
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

    public SocialUserResponse getNaverUserInfo(String accessToken) {
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

    // 사용자 소셜계정 로그아웃 및 회원탈퇴 처리
    private boolean logoutAndUnlinkSocialAccount(String requestType, User findUser, SocialType socialType){
        ResponseEntity<?> response = null;
        String jsonString = EMPTY;

        String socialId = StringUtils.defaultString(findUser.getSocialId());
        String socialAccessToken = StringUtils.defaultString(findUser.getSocialAccessToken());

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