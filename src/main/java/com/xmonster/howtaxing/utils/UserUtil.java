package com.xmonster.howtaxing.utils;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.user.UserRepository;
import com.xmonster.howtaxing.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserUtil {

    private final UserRepository userRepository;

    // (GGMANYAR) - TOBE
    public User findCurrentUser() {
        return userRepository.findBySocialId(SecurityUtil.getCurrentMemberSocialId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public Long findCurrentUserId() {
        return findCurrentUser().getId();
    }

    public String findCurrentUserSocialId(){
        return findCurrentUser().getSocialId();
    }

    public User findUserBySocialId(String socialId) {
        return userRepository.findBySocialId(socialId).orElse(null);
    }

    public User findUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public String findUserSocialIdByPhoneNumber(String phoneNumber) {
        User findUser = findUserByPhoneNumber(phoneNumber);

        if(findUser == null){
            throw new CustomException(ErrorCode.USER_NOT_FOUND, "가입된 회원의 휴대폰번호가 아닙니다.");
        }

        return findUser.getSocialId();
    }
}
