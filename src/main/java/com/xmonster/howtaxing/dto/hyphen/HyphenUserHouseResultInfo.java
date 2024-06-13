package com.xmonster.howtaxing.dto.hyphen;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HyphenUserHouseResultInfo implements Cloneable{
    private String resultListNo;        // 조회결과 목록번호(1:건축물대장정보 2:부동산거래내역 3:재산세정보)
    private String tradeType;           // 거래유형(0:없음 1:매수 2:매도)
    private String orgAdr;              // 원장주소
    private List<String> searchAdr;     // 검색주소
    private String detailAdr;           // 상세주소
    private String houseType;           // 주택유형(1:아파트 2:연립,다가구 3:입주권 4:단독주택,다세대 5:분양권(주택) 6:주택)
    private LocalDate contractDate;     // 계약일자
    private LocalDate balanceDate;      // 잔금지급일자
    private LocalDate buyDate;          // 취득일자
    private LocalDate sellDate;         // 양도일자
    private Long buyPrice;              // 취득금액
    private Long sellPrice;             // 양도금액
    private Long pubLandPrice;          // 공시지가
    private BigDecimal area;            // 전용면적

    @Override
    public HyphenUserHouseResultInfo clone() {
        try {
            // TODO: 이 복제본이 원본의 내부를 변경할 수 없도록 여기에 가변 상태를 복사합니다
            return (HyphenUserHouseResultInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
