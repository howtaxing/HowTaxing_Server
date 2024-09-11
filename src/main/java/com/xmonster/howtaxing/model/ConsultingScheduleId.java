package com.xmonster.howtaxing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Embeddable
@Getter
@Builder
public class ConsultingScheduleId implements Serializable {
    private Long consultantId;          // 상담자ID
    private LocalDate reservationDate;  // 예약일자
}
