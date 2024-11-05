package com.xmonster.howtaxing.dto.user;

import lombok.*;

// (GGMANYAR) - TOBE
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSignUpDto {
    private String joinType;
    private String id;
    private String password;
    private String email;
    //private String password;
    //private String nickname;
    //private int age;
    //private String city;
    private Boolean mktAgr;
}
