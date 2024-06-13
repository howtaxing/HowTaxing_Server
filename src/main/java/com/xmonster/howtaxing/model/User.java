package com.xmonster.howtaxing.model;

import com.xmonster.howtaxing.type.Role;
import com.xmonster.howtaxing.type.SocialType;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import javax.persistence.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Builder
@AllArgsConstructor
public class User extends DateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;                    // 사용자ID

    private String password;            // 비밀번호

    @Enumerated(EnumType.STRING)
    private SocialType socialType;      // SNS유형(0:EMAIL, 1:KAKAO, 2:NAVER, 3:GOOGLE, 4:APPLE, 5:ETC)

    private String socialId;            // SNS아이디(로그인 한 소셜 타입의 식별자 값 (일반 로그인인 경우 null))
    private String email;               // 이메일
    private String name;                // 이름
    private String nickname;            // 별명
    private Integer age;                // 나이
    private String city;                // 지역
    private String imageUrl;            // 이미지주소
    private String phoneNumber;         // 휴대폰번호

    @Enumerated(EnumType.STRING)
    private Role role;                  // 역할(0:GUEST, 1:USER)

    private String refreshToken;        // 리프레시 토큰(JWT)
    private boolean isMktAgr;           // 마케팅동의여부(true:여, false:부)
    private boolean isWithdraw;         // 탈퇴여부(true:여, false:부)

    // 유저 권한 설정
    public void authorizeUser() {
        this.role = Role.USER;
    }

    public void setMktAgr(boolean isMktAgr){
        this.isMktAgr = isMktAgr;
    }

    // 비밀번호 인코딩
    public void passwordEncode(PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(this.password);
    }
    
    public void updateNickname(String updateNickname) {
        this.nickname = updateNickname;
    }

    public void updateAge(int updateAge) {
        this.age = updateAge;
    }

    public void updateCity(String updateCity) {
        this.city = updateCity;
    }

    public void updatePassword(String updatePassword, PasswordEncoder passwordEncoder) {
        this.password = passwordEncoder.encode(updatePassword);
    }

    public void updateRefreshToken(String updateRefreshToken) {
        this.refreshToken = updateRefreshToken;
    }
}
