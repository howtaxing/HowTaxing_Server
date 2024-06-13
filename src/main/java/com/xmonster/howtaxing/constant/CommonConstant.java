package com.xmonster.howtaxing.constant;

public final class CommonConstant {

    public final static String EMPTY = "";
    public final static String SPACE = " ";
    public final static String HYPHEN = "-";
    public final static String NONE = "none";

    public final static String YES = "Y";
    public final static String NO = "N";

    public final static String ERR_YN = "errYn";
    public final static String ERR_MSG = "errMsg";

    public final static String ZERO = "0";
    public final static String ONE = "1";
    public final static String TWO = "2";
    public final static String THREE = "3";
    public final static String FOUR = "4";
    public final static String FIVE = "5";
    public final static String SIX = "6";
    public final static String SEVEN = "7";
    public final static String EIGHT = "8";
    public final static String NINE = "9";
    public final static String TEN = "10";

    /* 데이터 포맷 */
    public final static String JSON = "json";
    public final static String XML = "xml";

    /* 하이픈 관련 상수 */
    public final static String EASY = "EASY";

    public final static String INCLUDE = "01";
    public final static String NOT_INCLUDE = "02";

    public final static String INDVD_LOCAL = "01";
    public final static String CORPORATION = "02";
    public final static String INDVD_FOREIGNER = "03";

    public final static String PASS = "pass";
    public final static String KAKAO = "kakao";
    public final static String PAYCO = "payco";
    public final static String KICA = "kica";
    public final static String KB = "kb";

    public final static String SKT = "S";
    public final static String KT = "K";
    public final static String LGU = "L";

    public final static String INIT = "1";
    public final static String SIGN = "2";

    public final static String DEFAULT_DECIMAL = "0.0";
    public final static String DEFAULT_DATE = "00000000";

    public final static String MOVE_IN_KEYWORD = "전입";

    /* 계산 유형 */
    public final static String CALC_TYPE_BUY = "01";                // 취득세
    public final static String CALC_TYPE_SELL = "02";               // 양도소득세

    /* 데이터 함수 */
    public final static String BEFORE = "BEFORE";                   // YYYYMMDD일 이전
    public final static String OR_BEFORE = "OR_BEFORE";             // YYYYYMMDD일 포함 이전
    public final static String AFTER = "AFTER";                     // YYYYMMDD일 이후
    public final static String OR_AFTER = "OR_AFTER";               // YYYYMMDD일 포함 이후
    public final static String FROM_TO = "FROM_TO";                 // YYYYMMDD일 부터 YYYYMMDD일 까지
    public final static String LESS = "LESS";                       // 미만
    public final static String OR_LESS = "OR_LESS";                 // 이하
    public final static String MORE = "MORE";                       // 초과
    public final static String OR_MORE = "OR_MORE";                 // 이상
    public final static String WITHIN = "WITHIN";                   // n년이 된 날 이내
    public final static String WITHIN_YST = "WITHIN_YST";           // n년이 된 날 전날 이내
    public final static String WITHIN_TMR = "WITHIN_TMR";           // n년이 된 날 다음날 이내
    public final static String NOT_WITHIN = "NOT_WITHIN";           // n년이 된 날 이후
    public final static String NOT_WITHIN_YST = "NOT_WITHIN_YST";   // n년이 된 날 전날 이후
    public final static String NOT_WITHIN_TMR = "NOT_WITHIN_TMR";   // n년이 된 날 다음날 이후

    /* 데이터 유형 */
    public final static int DATA_TYPE_PRICE = 1;                    // 금액
    public final static int DATA_TYPE_DATE = 2;                     // 날짜
    public final static int DATA_TYPE_PERIOD = 3;                   // 기간

    /* 세율 유형 */
    public final static String GENERAL_TAX_RATE = "GEN";                    // 일반과세(일반세율)
    public final static String NONE_TAX_RATE = "NON";                       // 비과세
    public final static String NONE_AND_GENERAL_TAX_RATE = "NON_GEN";       // 기준금액 이하 비과세, 기준금액 이상 일반과세

    /* 세율 함수 */
    public final static String MAX = "MAX";                     // 세율1과 세율2 중 최대값 사용
    public final static String OR_LESS_MORE = "OR_LESS_MORE";   // 기준금액 이하 세율1, 기준금액 초과 세율2

    /* 취득세 일반세율 기준 금액 */
    public final static long ONE_HND_MIL = 100000000;           // 1억(원)
    public final static long SIX_HND_MIL = 600000000;           // 6억(원)
    public final static long NINE_HND_MIL = 900000000;          // 9억(원)

    /* 양도소득세 계산관련 금액 */
    public final static long BASIC_DEDUCTION_PRICE = 2500000;   // 기본공제액 250만(원)
    public final static double LOCAL_TAX_RATE = 0.1;            // 지방소득세율(양도소득세의 10%)
    public final static int PERIOD_YEAR = 365;                  // 기간(대략 1년의 일자)
    public final static int PERIOD_MONTH = 30;                  // 기간(대략 1개월의 일자)

    /* 질의 데이터 코드 */
    public final static String Q_0001 = "Q_0001";               // 질문1
    public final static String Q_0002 = "Q_0002";               // 질문2
    public final static String Q_0003 = "Q_0003";               // 질문3

    /* 답변 데이터 코드 */
    public final static String A_0001 = "A_0001";               // 답변1
    public final static String A_0002 = "A_0002";               // 답변2
    public final static String A_0003 = "A_0003";               // 답변3

    /* 공제단위 */
    public final static String UNIT_1YEAR = "1Y";                   // 1년

    /* 조정대상지역 (추후 DB로 관리 예정) */
    public final static String ADJUSTMENT_TARGET_AREA1 = "용산구";
    public final static String ADJUSTMENT_TARGET_AREA2 = "서초구";
    public final static String ADJUSTMENT_TARGET_AREA3 = "강남구";
    public final static String ADJUSTMENT_TARGET_AREA4 = "송파구";

    /* 전용면적(m2) */
    public final static double AREA_85 = 85;
}
