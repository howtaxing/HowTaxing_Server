package com.xmonster.howtaxing.dto.payment;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TossPaymentsConfirmResponse {
    private String version;                                 // Payment 객체의 응답 버전
    private String paymentKey;                              // 결제의 고유 키 값. 최대 길이는 200자이며, 결제 데이터 관리를 위해 반드시 저장해야 함
    private String type;                                    // 결제 타입 정보. NORMAL(일반결제), BILLING(자동결제), BRANDPAY(브랜드페이) 중 하나
    private String orderId;                                 // 주문번호. 가맹점이 생성한 고유한 문자열. 6자 이상 64자 이하
    private String orderName;                               // 구매 상품 이름. 예: "생수 외 1건". 최대 길이는 100자
    private String mId;                                     // 상점 아이디(MID). 토스페이먼츠에서 발급. 최대 길이는 14자
    private String currency;                                // 결제에 사용된 통화
    private String method;                                  // 결제수단. 카드, 가상계좌, 간편결제, 휴대폰, 계좌이체 등 중 하나
    private Long totalAmount;                               // 총 결제 금액. 결제가 취소되더라도 최초 결제 금액이 유지됨
    private Long balanceAmount;                             // 취소 가능한 잔액 금액. 결제 취소나 부분 취소 후 남은 금액
    private String status;                                  // 결제 처리 상태. READY, IN_PROGRESS, DONE 등 상태 값
    private String requestedAt;                             // 결제 요청 날짜 및 시간 정보. ISO 8601 형식
    private String approvedAt;                              // 결제 승인 날짜 및 시간 정보. ISO 8601 형식
    private Boolean useEscrow;                              // 에스크로 사용 여부
    private String lastTransactionKey;                      // 마지막 거래 키 값. 최대 길이는 200자
    private Long suppliedAmount;                            // 공급 가액. 부가세를 제외한 금액
    private Long vat;                                       // 부가세 금액. 일부 취소 시 값이 변경됨
    private Boolean cultureExpense;                         // 문화비 지출 여부. 도서, 공연 티켓 등에서 적용됨
    private Long taxFreeAmount;                             // 면세 금액. 면세 상점에서만 유효함
    private Integer taxExemptionAmount;                     // 과세 제외 금액. 컵 보증금 등에서 사용됨
    private List<Cancel> cancels;                           // 결제 취소 내역
    private Boolean isPartialCancelable;                    // 부분 취소 가능 여부
    private Card card;                                      // 카드 결제 정보
    private VirtualAccount virtualAccount;                  // 가상계좌 결제 정보
    private String secret;                                  // 웹훅 검증 값. 최대 50자
    private MobilePhone mobilePhone;                        // 휴대폰 결제 정보
    private GiftCertificate giftCertificate;                // 상품권 결제 정보
    private Transfer transfer;                              // 계좌이체 결제 정보
    private Object metadata;                                // 사용자 정의 데이터
    private Receipt receipt;                                // 영수증 정보
    private Checkout checkout;                              // 결제창 정보
    private EasyPay easyPay;                                // 간편결제 정보
    private String country;                                 // 결제한 국가. ISO-3166의 두 자리 국가 코드 형식
    private Failure failure;                                // 결제 실패 정보
    private CashReceipt cashReceipt;                        // 현금영수증 정보
    private List<CashReceipts> cashReceipts;                // 현금영수증 발행 및 취소 이력
    private Discount discount;                              // 즉시 할인 정보

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 결제 취소 내역
    public static class Cancel {
        private Long cancelAmount;                          // 취소된 금액
        private String cancelReason;                        // 취소 사유
        private Long taxFreeAmount;                         // 취소된 금액 중 면세 금액
        private Integer taxExemptionAmount;                 // 취소된 금액 중 과세 제외 금액
        private Long refundableAmount;                      // 환불 가능한 금액
        private Long transferDiscountAmount;                // 퀵계좌이체 즉시 할인 취소 금액
        private Long easyPayDiscountAmount;                 // 간편결제 할인 취소 금액
        private String canceledAt;                          // 취소 발생 시간. ISO 8601 형식
        private String transactionKey;                      // 취소 거래 키 값. 고유 식별자
        private String receiptKey;                          // 취소된 현금영수증 키 값
        private String cancelStatus;                        // 취소 상태. DONE이면 성공적으로 취소된 상태
        private String cancelRequestId;                     // 비동기 취소 요청 ID
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 카드 결제 정보
    public static class Card {
        private Long amount;                                // 카드사에 결제 요청한 금액
        private String issuerCode;                          // 카드 발급사 코드
        private String acquirerCode;                        // 카드 매입사 코드
        private String number;                              // 마스킹된 카드번호
        private Integer installmentPlanMonths;              // 할부 개월 수. 0이면 일시불
        private String approveNo;                           // 카드 승인 번호
        private Boolean useCardPoint;                       // 카드사 포인트 사용 여부
        private String cardType;                            // 카드 종류. 신용, 체크 등
        private String ownerType;                           // 카드 소유자 유형. 개인, 법인 등
        private String acquireStatus;                       // 카드 매입 상태
        private Boolean isInterestFree;                     // 무이자 할부 여부
        private String interestPayer;                       // 할부 이자 부담 주체
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 가상계좌 결제 정보
    public static class VirtualAccount {
        private String accountType;                         // 가상계좌 유형. 일반, 고정 중 하나
        private String accountNumber;                       // 발급된 계좌번호
        private String bankCode;                            // 은행 코드
        private String customerName;                        // 계좌 소유자 이름
        private String dueDate;                             // 입금 기한. ISO 8601 형식
        private String refundStatus;                        // 환불 처리 상태
        private Boolean expired;                            // 가상계좌 만료 여부
        private String settlementStatus;                    // 정산 상태
        private RefundReceiveAccount refundReceiveAccount;  // 환불 계좌 정보
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 환불 계좌 정보
    public static class RefundReceiveAccount {
        private String bankCode;                            // 은행 코드
        private String accountNumber;                       // 계좌번호
        private String holderName;                          // 계좌 예금주 이름
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 휴대폰 결제 정보
    public static class MobilePhone {
        private String customerMobilePhone;                 // 구매자 휴대폰 번호
        private String settlementStatus;                    // 정산 상태
        private String receiptUrl;                          // 영수증 URL
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 상품권 결제 정보
    public static class GiftCertificate {
        private String approveNo;                           // 승인 번호
        private String settlementStatus;                    // 정산 상태
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 계좌이체 결제 정보
    public static class Transfer {
        private String bankCode;                            // 은행 코드
        private String settlementStatus;                    // 정산 상태
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 영수증 정보
    public static class Receipt {
        private String url;                                 // 영수증 URL
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 결제창 정보
    public static class Checkout {
        private String url;                                 // 결제창 URL
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 간편결제 정보
    public static class EasyPay {
        private String provider;                            // 간편결제 제공자 코드
        private Long amount;                                // 간편결제 금액
        private Long discountAmount;                        // 간편결제 할인 금액
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 결제 실패 정보
    public static class Failure {
        private String code;                                // 오류 코드
        private String message;                             // 오류 메시지
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 현금영수증 정보
    public static class CashReceipt {
        private String type;                                // 현금영수증 종류
        private String receiptKey;                          // 현금영수증 키 값
        private String issueNumber;                         // 발급 번호
        private String receiptUrl;                          // 영수증 URL
        private Long amount;                                // 현금영수증 처리 금액
        private Long taxFreeAmount;                         // 면세 처리 금액
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 현금영수증 발행 및 취소 이력
    public static class CashReceipts {
        private String receiptKey;                          // 현금영수증의 키 값. 최대 길이는 200자
        private String orderId;                             // 주문번호. 각 주문을 식별하는 역할로 결제 상태가 변해도 유지됨
        private String orderName;                           // 구매 상품. 예: "생수 외 1건". 최대 길이는 100자
        private String type;                                // 현금영수증의 종류. 소득공제, 지출증빙 중 하나
        private String issueNumber;                         // 현금영수증 발급 번호. 최대 길이는 9자
        private String receiptUrl;                          // 발행된 현금영수증 URL
        private String businessNumber;                      // 사업자등록번호. 길이는 10자
        private String transactionType;                     // 현금영수증 발급 종류. 발급(CONFIRM) 또는 취소(CANCEL)
        private Long amount;                                // 현금영수증 처리 금액
        private Long taxFreeAmount;                         // 면세 처리 금액
        private String issueStatus;                         // 현금영수증 발급 상태. IN_PROGRESS, COMPLETED, FAILED 중 하나
        private Failure failure;                            // 결제 실패 객체. 오류 타입과 메시지를 포함
        private String customerIdentityNumber;              // 소비자 인증수단. 최대 길이는 30자
        private String requestedAt;                         // 현금영수증 발급 또는 취소 요청 시간. ISO 8601 형식
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    // 즉시 할인 정보
    public static class Discount {
        private Long amount;                                // 즉시 할인 적용된 금액
    }
}