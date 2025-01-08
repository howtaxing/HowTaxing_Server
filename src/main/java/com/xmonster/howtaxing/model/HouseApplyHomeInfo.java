package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 주택청약홈정보
 *  - 하이픈을 통해 청약홈에서 보유주택정보를 조회한 공통부 정보 저장
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HouseApplyHomeInfo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long searchId;                  // 조회ID

    private Long userId;                    // 사용자ID
    private String userTrNo;                // 사용자고유거래번호
    private String hyphenTrNo;              // 하이픈고유거래번호
    private String errYn;                   // 오류여부
    private String errCd;                   // 에러코드
    private String errMsg;                  // 응답메시지
    private LocalDateTime searchAt;         // 조회일시
}