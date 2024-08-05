package com.xmonster.howtaxing.controller.user;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.user.UserSignUpDto;
import com.xmonster.howtaxing.service.user.UserService;
import com.xmonster.howtaxing.type.ErrorCode;
import static com.xmonster.howtaxing.constant.CommonConstant.*;

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

    // 회원탈퇴
    @DeleteMapping("/user/withdraw")
    public Object withdraw() throws Exception {
        log.info(">> [Controller]UserController withdraw - 회원탈퇴");
        return userService.withdraw();
    }

    // 로그아웃
    @GetMapping("/user/logout")
    public Object logout() throws Exception {
        log.info(">> [Controller]UserController logout - 로그아웃");
        return userService.logout();
    }

    // (자동)로그인 성공
    @GetMapping("/oauth2/loginSuccess2")
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
    }

    @GetMapping("/oauth2/loginSuccess")
    public ResponseEntity<String> loginSuccess(@RequestParam String accessToken, @RequestParam String refreshToken, @RequestParam String role){
        log.info(">> [Controller]UserController loginSuccess - 로그인 성공");

        if(accessToken == null || refreshToken == null){
            throw new CustomException(ErrorCode.LOGIN_COMMON_ERROR);
        }

        String html = "<html><body><pre id='returnValue'>" +
                "{\"errYn\" : \"N\", \"data\" : " + "{\"accessToken\" : \"" + accessToken + "\", \"refreshToken\" : \"" + refreshToken + "\", \"role\" : \"" + role + "\"}}" +
                "</pre><script>window.onload = function() {" +
                "document.getElementById('returnValue').style.display = 'none';" +
                "};</script></body></html>";

        log.info("[GGMANYAR]html : " + html);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);

        return new ResponseEntity<>(html, headers, HttpStatus.OK);
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

    @GetMapping("/user/callback")
    public void userCallback(@RequestParam String userId, @RequestParam String referrerType){
        log.info(">> [Controller]UserController userCallback - 유저 콜백");

        log.info("userId : " + userId);
        log.info("referrerType : " + referrerType);
    }
}
