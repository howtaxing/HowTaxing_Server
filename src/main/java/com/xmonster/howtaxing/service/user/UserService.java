package com.xmonster.howtaxing.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.user.SocialLogoutAndUnlinkRequest;
import com.xmonster.howtaxing.dto.user.SocialLogoutAndUnlinkResponse;
import com.xmonster.howtaxing.dto.user.UserSignUpDto;
import com.xmonster.howtaxing.feign.kakao.KakaoUserApi;
import com.xmonster.howtaxing.feign.naver.NaverAuthApi;
import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.house.HouseRepository;
import com.xmonster.howtaxing.repository.user.UserRepository;
import com.xmonster.howtaxing.service.redis.RedisService;
import com.xmonster.howtaxing.type.ErrorCode;
import com.xmonster.howtaxing.type.SocialType;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

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
    private final NaverAuthApi naverAuthApi;
    private final RedisService redisService;
    //private final PasswordEncoder passwordEncoder;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverAppKey;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverAppSecret;

    // 회원가입
    public Object signUp(UserSignUpDto userSignUpDto) throws Exception {
        log.info(">> [Service]UserService signUp - 회원가입");

        Map<String, Object> resultMap = new HashMap<>();

        try{
            User findUser = userUtil.findCurrentUser();
            
            findUser.authorizeUser(); // 유저 권한 세팅(GUEST -> USER)
            findUser.setMktAgr(userSignUpDto.isMktAgr()); // 마케팅동의여부 세팅

            resultMap.put("role", findUser.getRole());


        }catch(Exception e){
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return ApiResponse.success(resultMap);
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
                if(SocialType.KAKAO.equals(socialType)){
                    response = kakaoUserApi.logoutUserInfo(
                            headerMap,
                            SocialLogoutAndUnlinkRequest.builder()
                                    .targetIdType("user_id")
                                    .targetId(Long.parseLong(socialId))
                                    .build());
                }else if(SocialType.NAVER.equals(socialType)){
                    log.info("네이버는 로그아웃 기능이 없습니다.");
                }else{
                    // google, apple..
                    throw new CustomException(ErrorCode.USER_LOGOUT_ERROR, "Google과 Apple의 로그아웃 기능은 준비 중입니다.");
                }
            }
            // 회원탈퇴
            else if(TWO.equals(requestType)){
                if(SocialType.KAKAO.equals(socialType)){
                    response = kakaoUserApi.unlinkUserInfo(
                            headerMap,
                            SocialLogoutAndUnlinkRequest.builder()
                                    .targetIdType("user_id")
                                    .targetId(Long.parseLong(socialId))
                                    .build());
                }else if(SocialType.NAVER.equals(socialType)){
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

        if(SocialType.KAKAO.equals(socialType)){
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