package com.xmonster.howtaxing.login.handler;

import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

/**
 * JWT 로그인 실패 시 처리하는 핸들러
 * SimpleUrlAuthenticationFailureHandler를 상속받아서 구현
 */
@Slf4j
@RequiredArgsConstructor
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UserRepository userRepository;

    // (GGMANYAR) - TOBE
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws ServletException, IOException {
        StringBuilder resultSb = new StringBuilder("/login/loginFail?socialType=IDPASS");
        String id = (String) request.getAttribute("id");
        String password = (String) request.getAttribute("password");

        User user = userRepository.findBySocialId(id).orElse(null);

        log.info("로그인에 실패했습니다. 메시지 : {}", exception.getMessage());

        if(user == null){
            resultSb.append("&error=");
            resultSb.append(NOT_FOUND);
            resultSb.append("&attemptFailedCount=");
            setDefaultFailureUrl(resultSb.toString());
            super.onAuthenticationFailure(request, response, exception);
        }else{
            int attemptFailedCount = (user.getAttemptFailedCount() != null) ?  user.getAttemptFailedCount() : 0;

            // 이미 실패 횟수가 5회 이상인 경우
            if(attemptFailedCount >= 5){
                resultSb.append("&error=");
                resultSb.append(LOCKED);
                resultSb.append("&attemptFailedCount=");
                resultSb.append(attemptFailedCount);
                setDefaultFailureUrl(resultSb.toString());
                super.onAuthenticationFailure(request, response, exception);
                return;
            }

            attemptFailedCount++;
            user.setAttemptFailedCount(attemptFailedCount);
            //user.incrementAttemptFailedCount();
            userRepository.save(user);

            // 실패 횟수가 5회가 된 경우
            if(attemptFailedCount == 5){
                user.setIsLocked(true);
                user.setLockedDatetime(LocalDateTime.now());
                userRepository.save(user);
                resultSb.append("&error=");
                resultSb.append(LOCKED);
                resultSb.append("&attemptFailedCount=");
                resultSb.append(attemptFailedCount);
                setDefaultFailureUrl(resultSb.toString());
                super.onAuthenticationFailure(request, response, exception);
                return;
            }

            // 실패 횟수가 5회 미만인 경우
            //int remainAttemptFailedCount = 5 - attemptFailedCount;
            resultSb.append("&error=");
            resultSb.append(ID_PASS_WRONG);
            resultSb.append("&attemptFailedCount=");
            resultSb.append(attemptFailedCount);
            setDefaultFailureUrl(resultSb.toString());
            super.onAuthenticationFailure(request, response, exception);
        }

        /*response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        //response.getWriter().write("로그인 실패! 아이디 또는 비밀번호를 확인해주세요.");
        response.getWriter().write("아이디 또는 비밀번호를 확인해주세요.");
        //log.info("로그인에 실패했습니다. 메시지 : {}", exception.getMessage());
        response.sendRedirect(resultSb.toString());*/

        //response.sendRedirect("/oauth2/loginFail?socialType=IDPASS&errorCode=" + exception.getMessage());
    }
}
