package com.xmonster.howtaxing.controller.user;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.user.UserSignUpDto;
import com.xmonster.howtaxing.service.user.UserService;
import com.xmonster.howtaxing.type.ErrorCode;
import static com.xmonster.howtaxing.constant.CommonConstant.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    // 회원가입
    @PostMapping("/user/signUp")
    public Object signUp(@RequestBody UserSignUpDto userSignUpDto) throws Exception {
        log.info(">> [Controller]UserController signUp - 회원가입");
        return userService.signUp(userSignUpDto);
    }

    // 회원탈퇴
    @DeleteMapping("/user/withdraw")
    public Object withdraw() throws Exception {
        log.info(">> [Controller]UserController withdraw - 회원탈퇴");
        return userService.withdraw();
    }

    // (자동)로그인 성공
    @GetMapping("/oauth2/loginSuccess")
    public Object loginSuccess(@RequestParam String accessToken, @RequestParam String refreshToken, @RequestParam String role){
        log.info(">> [Controller]UserController loginSuccess - 로그인 성공");

        Map<String, Object> tokenMap = new HashMap<>();

        if(accessToken == null || refreshToken == null){
            throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR);
        }

        tokenMap.put("accessToken", accessToken);
        tokenMap.put("refreshToken", refreshToken);
        tokenMap.put("role", role);

        return ApiResponse.success(tokenMap);
    }

    // (자동)로그인 실패
    @GetMapping("/oauth2/loginFail")
    public Object loginFail(@RequestParam String socialType){
        log.info(">> [Controller]UserController loginFail - 로그인 실패");

        if(socialType != null && !EMPTY.equals(socialType)){
            throw new CustomException(ErrorCode.LOGIN_HAS_EMAIL_ERROR, socialType + "를 통해 동일한 이메일이 가입되어 있습니다.");
        }else{
            throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR);
        }
    }
}
