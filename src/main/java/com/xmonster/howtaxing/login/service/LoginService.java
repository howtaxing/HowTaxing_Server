package com.xmonster.howtaxing.login.service;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.user.UserRepository;
import com.xmonster.howtaxing.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String socialId) throws UsernameNotFoundException {
        /*User user = userRepository.findBySocialId(socialId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 아이디가 존재하지 않습니다."));*/

        /*User user = userRepository.findBySocialId(socialId)
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_INVALID_PASSWORD, "해당 아이디가 존재하지 않습니다."));*/

        User user = userRepository.findBySocialId(socialId)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("아이디가 존재하지 않습니다."));

        if(user != null){
            log.info("[GGMANYAR]user.getSocialId : " + user.getSocialId());
            log.info("[GGMANYAR]user.getPassword : " + user.getPassword());
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getSocialId())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
