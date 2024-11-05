package com.xmonster.howtaxing.login.handler;

import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.user.UserRepository;
import com.xmonster.howtaxing.service.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${jwt.access.expiration}")
    private String accessTokenExpiration;

    // (GGMANYAR) - TOBE
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        //String email = extractUsername(authentication); // 인증 정보에서 Username(email) 추출
        String socialId = extractUsername(authentication); // 인증 정보에서 Username(socialId) 추출
        String accessToken = jwtService.createAccessToken(socialId); // JwtService의 createAccessToken을 사용하여 AccessToken 발급
        String refreshToken = jwtService.createRefreshToken(); // JwtService의 createRefreshToken을 사용하여 RefreshToken 발급

        jwtService.sendAccessAndRefreshToken(response, accessToken, refreshToken); // 응답 헤더에 AccessToken, RefreshToken 실어서 응답

        User user = userRepository.findBySocialId(socialId).orElse(null);
        if(user != null){
            user.updateRefreshToken(refreshToken);
            userRepository.saveAndFlush(user);
            String role = user.getRole().toString();

            response.addHeader(jwtService.getAccessHeader(), "Bearer " + accessToken);
            response.addHeader(jwtService.getRefreshHeader(), "Bearer " + refreshToken);

            log.info("로그인에 성공하였습니다. 아이디 : {}", socialId);
            log.info("로그인에 성공하였습니다. AccessToken : {}", accessToken);
            log.info("발급된 AccessToken 만료 기간 : {}", accessTokenExpiration);

            response.sendRedirect("/oauth2/loginSuccess?accessToken=" + accessToken + "&refreshToken=" + refreshToken + "&role=" + role);
        }

        /*userRepository.findBySocialId(socialId)
                .ifPresent(user -> {
                    user.updateRefreshToken(refreshToken);
                    userRepository.saveAndFlush(user);
                });*/
    }

    private String extractUsername(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }
}
