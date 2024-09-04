package com.xmonster.howtaxing.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ConsultingScheduleManagement extends DateEntity implements Serializable {
    @EmbeddedId
    private ConsultingScheduleId consultingScheduleId;

    private Boolean isReservationAvailable;         // 예약가능여부
    private String reservationAvailableStartTime;   // 예약가능시작시간
    private String reservationAvailableEndTime;     // 예약가능종료시간
    private Integer reservationTimeUnit;            // 예약시간단위(분단위)
    private String reservationUnavailableTime;      // 예약불가시간(콤마(,)로 구분)
}
