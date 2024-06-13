package com.xmonster.howtaxing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CalculationProcess extends DateEntity implements Serializable {
    @EmbeddedId
    private CalculationProcessId calculationProcessId;

    private String calcName;            // 계산명
    private String branchName;          // 분기명
    private String selectContent;       // 선택내용
    private String variableData;        // 가변데이터
    private String dataMethod;          // 데이터함수
    private boolean hasNextBranch;      // 다음분기존재여부
    private String nextBranchNo;        // 다음분기번호
    private String taxRateCode;         // 세율코드
    private String dedCode;             // 공제코드
    private String remark;              // 비고
}
