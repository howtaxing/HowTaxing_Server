package com.xmonster.howtaxing.dto.user;

import com.xmonster.howtaxing.type.SocialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserJoinRequest {
    private String socialId;
    //private String userName;
    private SocialType socialType;
    private String email;
}
