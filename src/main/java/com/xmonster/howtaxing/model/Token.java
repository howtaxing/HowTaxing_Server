package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDate;

import com.xmonster.howtaxing.type.TokenType;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Token extends DateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long tokenId;               // 토큰ID

    @Enumerated(EnumType.STRING)
    private TokenType tokenType;        // 토큰유형

    private String tokenName;           // 토큰명
    private String tokenValue;          // 토큰값
    private LocalDate tokenExpireDtm;   // 토큰만료일시
}