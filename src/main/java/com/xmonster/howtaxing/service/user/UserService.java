package com.xmonster.howtaxing.service.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.user.SocialUnlinkRequest;
import com.xmonster.howtaxing.dto.user.SocialUnlinkResponse;
import com.xmonster.howtaxing.dto.user.UserSignUpDto;
import com.xmonster.howtaxing.feign.kakao.KakaoUserApi;
import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.house.HouseRepository;
import com.xmonster.howtaxing.repository.user.UserRepository;
import com.xmonster.howtaxing.type.ErrorCode;
import com.xmonster.howtaxing.type.SocialType;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.xmonster.howtaxing.constant.CommonConstant.EMPTY;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final HouseRepository houseRepository;
    private final UserUtil userUtil;
    private final KakaoUserApi kakaoUserApi;
    //private final PasswordEncoder passwordEncoder;

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

        try{
            SocialType socialType = findUser.getSocialType();
            String socialId = StringUtils.defaultString(findUser.getSocialId());

            System.out.println("[GGMANYAR]socialType : " + socialType);
            System.out.println("[GGMANYAR]socialId : " + socialId);

            SocialUnlinkResponse socialUnlinkResponse = unlinkUserInfo(socialType, socialId);
            //return ApiResponse.success(socialUnlinkResponse);
            if(socialUnlinkResponse.getId() != null){
                log.info("[GGMANYAR]unlinked Id : " + socialUnlinkResponse.getId());
                if(socialUnlinkResponse.getId().equals(Long.parseLong(socialId))){
                    userRepository.deleteById(findUser.getId());        // 회원 정보 삭제
                    //userRepository.deleteByEmail(findUser.getEmail());
                    houseRepository.deleteByUserId(findUser.getId());   // 회원의 주택 정보 삭제
                }else{
                    throw new CustomException(ErrorCode.USER_NOT_FOUND, "회원탈퇴 중 오류가 발생했습니다.");
                }
            }
        }catch(Exception e){
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return ApiResponse.success(Map.of("result", "회원탈퇴가 완료되었습니다."));
    }

    private SocialUnlinkResponse unlinkUserInfo(SocialType socialType, String socialId){
        ResponseEntity<?> response = null;
        String jsonString = EMPTY;

        try{
            if(SocialType.KAKAO.equals(socialType)){
                response = kakaoUserApi.unlinkUserInfo(
                        SocialUnlinkRequest.builder()
                                .targetIdType("user_id")
                                .targetId(Long.parseLong(socialId))
                                .build());
            }else if(SocialType.NAVER.equals(socialType)){
                throw new CustomException(ErrorCode.ETC_ERROR, "아직 네이버 회원탈퇴는 준비 중입니다.");
            }else{
                // google, apple..
            }
        }catch(Exception e){
            log.error("회원탈퇴 처리중 오류 발생 : " + e.getMessage());
            throw new CustomException(ErrorCode.ETC_ERROR);
        }

        log.info("social unlink response");
        if(response != null && response.getBody() != null){
            jsonString = response.getBody().toString();
            log.info(response.getBody().toString());
        }

        return (SocialUnlinkResponse) convertJsonToData(jsonString);
    }

    private Object convertJsonToData(String jsonString) {
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonString, SocialUnlinkResponse.class);
        }catch(Exception e){
            log.error(e.getMessage());
            throw new CustomException(ErrorCode.ETC_ERROR);
        }
    }
}