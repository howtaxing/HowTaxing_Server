package com.xmonster.howtaxing.controller.user;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.sms.SmsCheckAuthCodeRequest;
import com.xmonster.howtaxing.dto.user.*;
import com.xmonster.howtaxing.service.user.UserService;
import com.xmonster.howtaxing.type.ErrorCode;
import static com.xmonster.howtaxing.constant.CommonConstant.*;

import com.xmonster.howtaxing.type.SocialType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    // 아이디 중복체크
    @GetMapping("/user/idCheck")
    public Object idDuplicateCheck(@RequestParam String id) throws Exception {
        log.info(">> [Controller]UserController idDuplicateCheck - 아이디 중복체크");
        return userService.idDuplicateCheck(id);
    }

    // 회원탈퇴
    @DeleteMapping("/user/withdraw")
    public Object withdraw() throws Exception {
        log.info(">> [Controller]UserController withdraw - 회원탈퇴");
        return userService.withdraw();
    }

    // 로그인(아이디/비밀번호)
    @PostMapping("/user/login")
    public Object login(@RequestBody UserLoginDto userLoginDto) throws Exception {
        log.info(">> [Controller]UserController login - 로그인");
        return userService.login(userLoginDto);
    }

    // 로그아웃
    @GetMapping("/user/logout")
    public Object logout() throws Exception {
        log.info(">> [Controller]UserController logout - 로그아웃");
        return userService.logout();
    }

    // 로그인(아이디/비밀번호)
    @PostMapping("/user/socialLogin")
    public Object socialLogin(@RequestBody SocialLoginRequest socialLoginRequest) throws Exception {
        log.info(">> [Controller]UserController socialLogin - 소셜로그인");
        return userService.socialLogin(socialLoginRequest);
    }

    /*@GetMapping("/oauth2/loginSuccess2")
    public Object loginSuccess2(@RequestParam String accessToken, @RequestParam String refreshToken, @RequestParam String role){
        log.info(">> [Controller]UserController loginSuccess - 로그인 성공");

        Map<String, Object> tokenMap = new HashMap<>();

        if(accessToken == null || refreshToken == null){
            throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR);
        }

        tokenMap.put("accessToken", accessToken);
        tokenMap.put("refreshToken", refreshToken);
        tokenMap.put("role", role);

        return ApiResponse.success(tokenMap);
    }*/

    // 소셜 로그인 성공
    @GetMapping("/oauth2/loginSuccess")
    public ResponseEntity<String> socialLoginSuccess(@RequestParam String accessToken, @RequestParam String refreshToken, @RequestParam String role){
        log.info(">> [Controller]UserController socialLoginSuccess - 소셜 로그인 성공");

        if(accessToken == null || refreshToken == null){
            throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR);
        }

        String html = "<html><body><pre id='returnValue'>" +
                "{\"errYn\" : \"N\", \"data\" : " + "{\"accessToken\" : \"" + accessToken + "\", \"refreshToken\" : \"" + refreshToken + "\", \"role\" : \"" + role + "\"}}" +
                "</pre><script>window.onload = function() {" +
                "document.getElementById('returnValue').style.display = 'none';" +
                "};</script></body></html>";

        log.info("html : " + html);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);

        return new ResponseEntity<>(html, headers, HttpStatus.OK);
    }

    // 소셜 로그인 실패
    @GetMapping("/oauth2/loginFail")
    public ResponseEntity<String> socialLoginFail(){
        log.info(">> [Controller]UserController socialLoginFail - 소셜 로그인 실패");

        String html = "<html><body><pre id='returnValue'>" +
                "{\"errYn\": \"Y\", \"type\": 1, \"status\": 200, \"name\": \"LOGIN_COMMON_ERROR\", \"errCode\": \"LOGIN-001\", \"errMsg\": \"로그인 중 오류가 발생했습니다.\", \"errMsgDtl\": \"\"}" +
                "</pre><script>window.onload = function() {" +
                "document.getElementById('returnValue').style.display = 'none';" +
                "};</script></body></html>";

        log.info("html : " + html);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);

        return new ResponseEntity<>(html, headers, HttpStatus.OK);
    }

    // 일반 로그인 성공
    /*@GetMapping("/login/loginSuccess")
    public ResponseEntity<String> generalLoginSuccess(@RequestParam String accessToken, @RequestParam String refreshToken, @RequestParam String role){
        log.info(">> [Controller]UserController loginSuccess - 로그인 성공");

        if(accessToken == null || refreshToken == null){
            throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR);
        }

        // 세션에 사용자정보 저장
        // userService.saveUserSession(accessToken);

        String html = "<html><body><pre id='returnValue'>" +
                "{\"errYn\" : \"N\", \"data\" : " + "{\"accessToken\" : \"" + accessToken + "\", \"refreshToken\" : \"" + refreshToken + "\", \"role\" : \"" + role + "\"}}" +
                "</pre><script>window.onload = function() {" +
                "document.getElementById('returnValue').style.display = 'none';" +
                "};</script></body></html>";

        log.info("html : " + html);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);

        return new ResponseEntity<>(html, headers, HttpStatus.OK);
    }*/

    // 일반 로그인 성공
    @GetMapping("/login/loginSuccess")
    public Object generalLoginSuccess(@RequestParam String accessToken, @RequestParam String refreshToken, @RequestParam String role){
        log.info(">> [Controller]UserController generalLoginSuccess - 일반 로그인 성공");

        Map<String, Object> tokenMap = new HashMap<>();

        if(accessToken == null || refreshToken == null){
            throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR);
        }

        tokenMap.put("accessToken", accessToken);
        tokenMap.put("refreshToken", refreshToken);
        tokenMap.put("role", role);

        return ApiResponse.success(tokenMap);
    }

    // 일반 로그인 실패
    @GetMapping("/login/loginFail")
    public Object generalLoginFail(@RequestParam String error, @RequestParam String attemptFailedCount){
        log.info(">> [Controller]UserController generalLoginFail - 일반 로그인 실패");

        log.info("error : " + error);
        log.info("attemptFailedCount : " + attemptFailedCount);

        if(error != null && !EMPTY.equals(error)) {
            if(ID_PASS_WRONG.equals(error)){
                throw new CustomException(ErrorCode.LOGIN_INVALID_PASSWORD, attemptFailedCount + "/5 회 오류");
            }else if (NOT_FOUND.equals(error)){
                throw new CustomException(ErrorCode.LOGIN_ID_NOT_EXIST);
            }else if(LOCKED.equals(error)) {
                throw new CustomException(ErrorCode.LOGIN_ACCOUNT_LOCKED, "5분 후에 다시 시도하세요.");
            }else{
                throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR);
            }
        }else{
            throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR);
        }
    }

    // 아이디 찾기(일반로그인)
    @PostMapping("/user/findUserId")
    public Object findUserId(@RequestBody UserFindIdRequest userFindIdRequest) throws Exception {
        log.info(">> [Controller]UserController findUserId - 아이디 찾기(일반로그인)");
        return userService.findUserId(userFindIdRequest);
    }

    // 비밀번호 재설정(일반로그인)
    @PostMapping("/user/resetPassword")
    public Object resetPassword(@RequestBody UserResetPasswordRequest userResetPasswordRequest) throws Exception {
        log.info(">> [Controller]UserController resetPassword - 비밀번호 재설정(일반로그인)");
        return userService.resetPassword(userResetPasswordRequest);
    }

    @GetMapping("/user/callback")
    public void userCallback(@RequestParam String userId, @RequestParam String referrerType){
        log.info(">> [Controller]UserController userCallback - 유저 콜백");

        log.info("userId : " + userId);
        log.info("referrerType : " + referrerType);
    }
}
