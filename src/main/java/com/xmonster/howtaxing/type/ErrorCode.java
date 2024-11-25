package com.xmonster.howtaxing.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    /* 사용자 관련 */
    USER_NOT_FOUND(1, HttpStatus.OK, "USER-001", "사용자를 찾을 수 없습니다."),
    USER_LOGOUT_ERROR(1, HttpStatus.OK, "USER-005", "로그아웃 처리 중 오류가 발생했습니다."),
    USER_WITHDRAW_ERROR(1, HttpStatus.OK, "USER-006", "회원탈퇴 처리 중 오류가 발생했습니다."),

    /* 로그인 관련 */
    LOGIN_COMMON_ERROR(1, HttpStatus.OK, "LOGIN-001", "로그인 중 오류가 발생했습니다."),
    LOGIN_INVALID_PASSWORD(1, HttpStatus.OK, "LOGIN-002", "아이디 또는 비밀번호가 일치하지 않아요."),
    LOGIN_ID_NOT_EXIST(1, HttpStatus.OK, "LOGIN-003", "입력하신 아이디가 존재하지 않아요."),
    LOGIN_ACCOUNT_LOCKED(1, HttpStatus.OK, "LOGIN-004", "비밀번호가 5회 불일치로 계정이 잠겼어요."),

    /* 회원가입 관련 */
    JOIN_USER_INPUT_ERROR(1, HttpStatus.OK, "JOIN-001", "회원가입을 위한 입력값이 올바르지 않습니다."),
    JOIN_USER_OUTPUT_ERROR(1, HttpStatus.OK, "JOIN-002", "회원가입 중 오류가 발생했습니다."),
    JOIN_USER_ID_EXIST(1, HttpStatus.OK, "JOIN-003", "이미 존재하는 아이디 입니다."),
    JOIN_PHONENUMBER_DUPLICATE(1, HttpStatus.OK, "JOIN-004", "이미 사용 중인 휴대폰 번호에요."),

    /* 아이디 중복체크 관련 */
    ID_CHECK_INPUT_ERROR(1, HttpStatus.OK, "IDCHECK-001", "아이디 중복체크를 위한 입력값이 올바르지 않습니다."),
    ID_CHECK_ALREADY_EXIST(1, HttpStatus.OK, "IDCHECK-002", "이미 존재하는 아이디 입니다."),

    /* 아이디 찾기 관련 */
    ID_FIND_AUTH_ERROR(1, HttpStatus.OK, "FIND-001", "아이디 찾기를 위한 인증키 검증에 실패했어요."),
    ID_FIND_INPUT_ERROR(1, HttpStatus.OK, "FIND-002", "아이디 찾기를 위한 요청 값이 올바르지 않아요."),
    ID_FIND_MESSAGE_ERROR(1, HttpStatus.OK, "FIND-003", "아이디 정보를 전달할 메시지 발송에 실패했어요"),

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

    /* 공시가격 및 전용면적 관련 */
    HOUSE_VWORLD_INPUT_ERROR(1, HttpStatus.OK, "HOUSE-010", "공시가격 및 전용면적 조회를 위한 요청값이 올바르지 않습니다."),
    HOUSE_VWORLD_OUTPUT_ERROR(1, HttpStatus.OK, "HOUSE-011", "공공기관에서 공시가격 및 전용면적 정보를 가져오는 중 오류가 발생했습니다."),
    HOUSE_VWORLD_SYSTEM_ERROR(1, HttpStatus.OK, "HOUSE-012", "공공기관의 시스템에 문제가 발생하여 공시가격 및 전용면적 정보를 가져오는 중 오류가 발생했습니다."),

    /* 주택(내부 데이터) 관련 */
    HOUSE_NOT_FOUND_ERROR(1, HttpStatus.OK, "HOUSE-013", "해당 주택 정보를 찾을 수 없습니다."),
    HOUSE_REGIST_ERROR(1, HttpStatus.OK, "HOUSE-014", "보유주택 등록 중 오류가 발생했습니다."),
    HOUSE_MODIFY_ERROR(1, HttpStatus.OK, "HOUSE-015", "보유주택 수정 중 오류가 발생했습니다."),
    HOUSE_DELETE_ERROR(1, HttpStatus.OK, "HOUSE-016", "보유주택 삭제 중 오류가 발생했습니다."),
    HOUSE_DELETE_ALL_ERROR(1, HttpStatus.OK, "HOUSE-017", "보유주택 전체 삭제 중 오류가 발생했습니다."),

    /* 주택(청약홈 로드 데이터) 관련 */
    HOUSE_GET_INFO_ERROR(1, HttpStatus.OK, "HOUSE-018", "보유주택 정보 조회를 위한 필수 입력값이 올바르지 않습니다."),
    HOUSE_GET_INFO_NOT_FOUND(1, HttpStatus.OK, "HOUSE-019", "보유주택 정보 조회에 실패하였습니다."),

    /* 주택(보유주택 조회) 관련 */
    HOUSE_HYPHEN_RLNO_ERROR(1, HttpStatus.OK, "HOUSE-020", "청약통장이 없거나 주민등록번호가 잘못 입력되어 청약홈 인증에 실패하였습니다."),
    HOUSE_HYPHEN_ACCOUNT_ERROR(1, HttpStatus.OK, "HOUSE-021", "해당 간편인증 회원이 아니거나 아이디 또는 패스워드가 틀려 청약홈 인증에 실패했습니다."),

    /* 주소 관련 오류 */
    ADDRESS_SEPARATE_ERROR(1, HttpStatus.OK, "ADDRESS-001", "주소 데이터 프로세스 중 오류가 발생했습니다."),
    
    /* 추가질의 항목 조회 관련 */
    QUESTION_INPUT_ERROR(1, HttpStatus.OK, "QUESTION-001", "추가질의항목 조회를 위한 입력값이 올바르지 않습니다."),
    QUESTION_OUTPUT_NOT_FOUND(1, HttpStatus.OK, "QUESTION-002", "추가질의항목이 존재하지 않습니다."),
    QUESTION_OUTPUT_ERROR(1, HttpStatus.OK, "QUESTION-003", "추가질의항목 조회 중 오류가 발생했습니다."),

    /* 계산 관련 */
    CALCULATION_BUY_TAX_FAILED(2, HttpStatus.OK, "CALCULATION-001", "취득세 계산 중 오류가 발생했습니다."),
    CALCULATION_SELL_TAX_FAILED(2, HttpStatus.OK, "CALCULATION-002", "양도소득세 계산 중 오류가 발생했습니다."),

    /* 상담 관련 */
    CONSULTING_SCHEDULE_INPUT_ERROR(1, HttpStatus.OK, "CONSULTING-001", "상담가능일정 조회를 위한 입력값이 올바르지 않습니다."),
    CONSULTING_SCHEDULE_OUTPUT_ERROR(1, HttpStatus.OK, "CONSULTING-002", "상담가능일정 조회 중 오류가 발생했습니다."),
    CONSULTING_APPLY_INPUT_ERROR(1, HttpStatus.OK, "CONSULTING-003", "상담 예약 신청을 위한 입력값이 올바르지 않습니다."),
    CONSULTING_APPLY_OUTPUT_ERROR(1, HttpStatus.OK, "CONSULTING-004", "상담 예약 신청 중 오류가 발생했습니다."),
    CONSULTING_MODIFY_INPUT_ERROR(1, HttpStatus.OK, "CONSULTING-005", "상담 예약 변경을 위한 입력값이 올바르지 않습니다."),
    CONSULTING_MODIFY_OUTPUT_ERROR(1, HttpStatus.OK, "CONSULTING-006", "상담 예약 변경 중 오류가 발생했습니다."),
    CONSULTING_CANCEL_INPUT_ERROR(1, HttpStatus.OK, "CONSULTING-007", "상담 예약 취소를 위한 입력값이 올바르지 않습니다."),
    CONSULTING_CANCEL_OUTPUT_ERROR(1, HttpStatus.OK, "CONSULTING-008", "상담 예약 취소 중 오류가 발생했습니다."),
    CONSULTING_LIST_INPUT_ERROR(1, HttpStatus.OK, "CONSULTING-009", "상담 예약 목록 조회를 위한 입력값이 올바르지 않습니다."),
    CONSULTING_LIST_OUTPUT_ERROR(1, HttpStatus.OK, "CONSULTING-010", "상담 예약 목록 조회 중 오류가 발생했습니다."),
    CONSULTING_DETAIL_INPUT_ERROR(1, HttpStatus.OK, "CONSULTING-011", "상담 예약 상세 조회를 위한 입력값이 올바르지 않습니다."),
    CONSULTING_DETAIL_OUTPUT_ERROR(1, HttpStatus.OK, "CONSULTING-012", "상담 예약 상세 조회 중 오류가 발생했습니다."),
    CONSULTING_RESERVATION_DUPLICATED_ERROR(1, HttpStatus.OK, "CONSULTING-013", "선택하신 날짜와 시간에 이미 예약된 내역이 있습니다."),
    CONSULTING_RESERVATION_ALREADY_ERROR(1, HttpStatus.OK, "CONSULTING-014", "선택하신 날짜에 이미 예약된 내역이 있으면 추가로 예약할 수 없습니다."),
    CONSULTING_CONSULTANT_INPUT_ERROR(1, HttpStatus.OK, "CONSULTING-015", "상담자 정보 상세 조회를 위한 입력값이 올바르지 않습니다."),

    /* 리뷰 관련 */
    REVIEW_REGIST_ERROR(1, HttpStatus.OK, "REVIEW-001", "리뷰 등록 중 오류가 발생했습니다."),

    /* 메시지 관련 */
    SMS_AUTH_INPUT_ERROR(1, HttpStatus.OK, "MESSAGE-001", "인증번호 발송 입력값이 올바르지 않아요."),
    SMS_AUTH_COUNT_ERROR(1, HttpStatus.OK, "MESSAGE-002", "인증번호는 하루에 10회까지만 발송할 수 있어요."),
    SMS_AUTH_SEND_ERROR(1, HttpStatus.OK, "MESSAGE-003", "인증번호 발송에 실패했어요."),
    SMS_AUTH_CHECK_ERROR(1, HttpStatus.OK, "MESSAGE-004", "인증번호 검증에 실패했어요."),
    SMS_AUTH_MATCH_ERROR(1, HttpStatus.OK, "MESSAGE-005", "인증번호가 정확하지 않아요."),
    SMS_AUTH_TIME_ERROR(1, HttpStatus.OK, "MESSAGE-006", "인증시간이 만료되었어요."),
    SMS_MSG_INPUT_ERROR(1, HttpStatus.OK, "MESSAGE-007", "SMS 발송을 위한 입력값이 올바르지 않아요."),
    SMS_MSG_OUTPUT_ERROR(1, HttpStatus.OK, "MESSAGE-008", "SMS 발송 중 오류가 발생했어요."),
    SMS_MSG_SYSTEM_ERROR(1, HttpStatus.OK, "MESSAGE-009", "SMS 시스템에 오류가 발생하여 메시지를 발송할 수 없어요."),

    /* 시스템 관련 */
    SYSTEM_UNKNOWN_ERROR(2, HttpStatus.OK, "SYSTEM-001", "알 수 없는 오류가 발생했습니다."),

    /* 기타 */
    ETC_ERROR(2, HttpStatus.OK, "ETC-001", "오류가 발생했습니다.");

    private final int type;                 // (오류)유형 (1:단순 오류 메시지, 2:상담 연결 메시지)
    private final HttpStatus httpStatus;	// HttpStatus (400, 404, 500...)
    private final String code;				// (오류)코드 ("ACCOUNT-001")
    private final String message;			// (오류)설명 ("사용자를 찾을 수 없습니다.")
}