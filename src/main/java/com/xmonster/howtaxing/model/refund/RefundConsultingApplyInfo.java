package com.xmonster.howtaxing.model.refund;

import com.xmonster.howtaxing.model.DateEntity;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RefundConsultingApplyInfo extends DateEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long refundConsultingApplyId;       // 환급상담신청ID

    private Long userId;                        // 사용자ID
    private String customerPhone;               // 고객전화번호
    private Boolean isRefundAvailable;          // 환급대상여부
    private Boolean isConsultingCompleted;      // 상담완료여부
}