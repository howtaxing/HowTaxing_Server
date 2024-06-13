package com.xmonster.howtaxing.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    /* 사용자 관련 */
    USER_NOT_FOUND(1, HttpStatus.OK, "ACCOUNT-001", "사용자를 찾을 수 없습니다."),

    /* 로그인 관련 */
    LOGIN_COMMON_ERROR(1, HttpStatus.OK, "LOGIN-001", "로그인 중 오류가 발생했습니다."),
    LOGIN_HAS_EMAIL_ERROR(1, HttpStatus.OK, "LOGIN-002", "이미 동일한 이메일 계정으로 가입되어 있습니다."),
    INVALID_PASSWORD(1, HttpStatus.OK, "LOGIN-003", "비밀번호가 일치하지 않습니다."),

    /* 주택(취득주택 조회) 관련 */
    HOUSE_JUSOGOV_INPUT_ERROR(1, HttpStatus.OK, "HOUSE-001", "주택 정보 조회를 위한 요청값이 올바르지 않습니다."),
    HOUSE_JUSOGOV_OUTPUT_ERROR(1, HttpStatus.OK, "HOUSE-002", "공공기관에서 검색한 주택 정보를 가져오는 중 오류가 발생했습니다."),
    HOUSE_JUSOGOV_SYSTEM_ERROR(1, HttpStatus.OK, "HOUSE-003", "공공기관의 시스템에 문제가 발생하여 검색한 주택 정보를 가져오는 중 오류가 발생했습니다."),

    /* 주택(보유주택 조회) 관련 */
    HOUSE_HYPHEN_INPUT_ERROR(1, HttpStatus.OK, "HOUSE-004", "보유주택 정보 조회를 위한 간편인증 입력값이 올바르지 않습니다."),
    HOUSE_HYPHEN_OUTPUT_ERROR(1, HttpStatus.OK, "HOUSE-005", "공공기관에서 보유주택 정보를 가져오는 중 오류가 발생했습니다."),
    HOUSE_HYPHEN_SYSTEM_ERROR(1, HttpStatus.OK, "HOUSE-006", "공공기관의 시스템에 문제가 발생하여 보유주택 정보를 가져오는 중 오류가 발생했습니다."),

    /* 주택(양도주택 거주기간 조회) 관련 */
    HYPHEN_STAY_PERIOD_INPUT_ERROR(1, HttpStatus.OK, "HOUSE-007", "주택 거주기간 조회를 위한 입력값이 올바르지 않습니다."),
    HYPHEN_STAY_PERIOD_OUTPUT_ERROR(1, HttpStatus.OK, "HOUSE-008", "공공기관에서 거주기간 정보를 가져오는 중 오류가 발생했습니다."),
    HYPHEN_STAY_PERIOD_SYSTEM_ERROR(1, HttpStatus.OK, "HOUSE-009", "공공기관의 시스템에 문제가 발생하여 거주기간 정보를 가져오는 중 오류가 발생했습니다."),

    /* 주택(내부 데이터) 관련 */
    HOUSE_NOT_FOUND_ERROR(1, HttpStatus.OK, "HOUSE-010", "해당 주택 정보를 찾을 수 없습니다."),
    HOUSE_REGIST_ERROR(1, HttpStatus.OK, "HOUSE-011", "보유주택 등록 중 오류가 발생했습니다."),
    HOUSE_MODIFY_ERROR(1, HttpStatus.OK, "HOUSE-012", "보유주택 수정 중 오류가 발생했습니다."),
    HOUSE_DELETE_ERROR(1, HttpStatus.OK, "HOUSE-013", "보유주택 삭제 중 오류가 발생했습니다."),
    HOUSE_DELETE_ALL_ERROR(1, HttpStatus.OK, "HOUSE-014", "보유주택 전체 삭제 중 오류가 발생했습니다."),

    /* 계산 관련 */
    CALCULATION_BUY_TAX_FAILED(2, HttpStatus.OK, "CALCULATION-001", "취득세 계산 중 오류가 발생했습니다."),
    CALCULATION_SELL_TAX_FAILED(2, HttpStatus.OK, "CALCULATION-002", "양도소득세 계산 중 오류가 발생했습니다."),

    /* 리뷰 관련 */
    REVIEW_REGIST_ERROR(1, HttpStatus.OK, "REVIEW-001", "리뷰 등록 중 오류가 발생했습니다."),

    /* 시스템 관련 */
    SYSTEM_UNKNOWN_ERROR(2, HttpStatus.OK, "SYSTEM-001", "알 수 없는 오류가 발생했습니다."),

    /* 기타 */
    ETC_ERROR(2, HttpStatus.OK, "ETC-001", "오류가 발생했습니다.");

    private final int type;                 // (오류)유형 (1:단순 오류 메시지, 2:상담 연결 메시지)
    private final HttpStatus httpStatus;	// HttpStatus (400, 404, 500...)
    private final String code;				// (오류)코드 ("ACCOUNT-001")
    private final String message;			// (오류)설명 ("사용자를 찾을 수 없습니다.")
}