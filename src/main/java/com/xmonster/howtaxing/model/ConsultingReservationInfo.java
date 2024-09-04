package com.xmonster.howtaxing.model;

import com.xmonster.howtaxing.type.ConsultingStatus;
import com.xmonster.howtaxing.type.LastModifierType;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ConsultingReservationInfo extends DateEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long consultingReservationId;               // 상담예약ID

    private Long consultantId;                          // 상담자ID
    private Long userId;                                // 사용자ID
    private Long calcHistoryId;                         // 계산이력ID

    private String consultingType;                      // 상담유형(콤마(,)로 구분(01:취득세 02:양도소득세 03:상속세 04:재산세))
    private LocalDate reservationDate;                  // 예약일자
    private LocalTime reservationStartTime;             // 예약시작시간
    private LocalTime reservationEndTime;               // 예약종료시간
    private String customerName;                        // 고객명
    private String customerPhone;                       // 고객전화번호
    private String consultingInflowPath;                // 상담유입경로(00:일반 01:취득세계산 02:양도소득세계산)
    private String consultingRequestContent;            // 상담요청내용

    @Enumerated(EnumType.STRING)
    private ConsultingStatus consultingStatus;          // 상담진행상태

    private Long customerFeesPrice;                     // 고객수수료금액
    private String consultingContent;                   // 상담내용
    private String remark;                              // 비고
    private Boolean isCanceled;                         // 취소여부
    private LocalDateTime consultingRequestDatetime;    // 상담요청일시
    private LocalDateTime consultingStartDatetime;      // 상담시작일시
    private LocalDateTime consultingEndDatetime;        // 상담종료일시
    private LocalDateTime consultingCancelDatetime;     // 상담취소일시
    private LastModifierType lastModifier;              // 최종변경자
}
