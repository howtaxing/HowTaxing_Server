package com.xmonster.howtaxing.dto.user;

import lombok.*;

// (GGMANYAR) - TOBE
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLoginDto {
    private String id;
    private String password;
}
