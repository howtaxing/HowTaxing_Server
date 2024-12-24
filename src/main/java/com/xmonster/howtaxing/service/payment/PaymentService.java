package com.xmonster.howtaxing.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.consulting.ConsultingReservationCreateRequest;
import com.xmonster.howtaxing.dto.payment.*;
import com.xmonster.howtaxing.feign.tosspayments.PaymentsConfirmApi;
import com.xmonster.howtaxing.model.ConsultantInfo;
import com.xmonster.howtaxing.model.ConsultingReservationInfo;
import com.xmonster.howtaxing.model.PaymentHistory;
import com.xmonster.howtaxing.repository.consulting.ConsultingReservationInfoRepository;
import com.xmonster.howtaxing.repository.payment.PaymentHistoryRepository;
import com.xmonster.howtaxing.service.consulting.ConsultingService;
import com.xmonster.howtaxing.type.ConsultingStatus;
import com.xmonster.howtaxing.type.ErrorCode;
import com.xmonster.howtaxing.type.PaymentStatus;
import com.xmonster.howtaxing.utils.ConsultantUtil;
import com.xmonster.howtaxing.utils.ConsultingReservationUtil;
import com.xmonster.howtaxing.utils.PaymentUtil;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.xmonster.howtaxing.constant.CommonConstant.*;
import static com.xmonster.howtaxing.constant.CommonConstant.CONSULTING_TYPE_PROPERTY;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final ConsultingReservationInfoRepository consultingReservationInfoRepository;

    private final ConsultingService consultingService;

    private final UserUtil userUtil;
    private final ConsultingReservationUtil consultingReservationUtil;
    private final ConsultantUtil consultantUtil;
    private final PaymentUtil paymentUtil;

    private final PaymentsConfirmApi paymentsConfirmApi;

    // 결제요청 정보 임시저장
    public Object saveTempPaymentRequestInfo(TempPaymentRequestInfoRequest tempPaymentRequestInfoRequest) throws Exception {
        log.info(">> [Service]PaymentService saveTempPaymentRequestInfo - 결제요청정보 임시저장");

        // 결제요청정보 임시저장 유효성 검증
        validationCheckForSaveTempPaymentRequestInfo(tempPaymentRequestInfoRequest);

        log.info("결제요청 정보 임시저장 : " + tempPaymentRequestInfoRequest);

        // 상담예약정보 생성
        Long consultingReservationId = consultingService.createConsultingReservation(
                ConsultingReservationCreateRequest.builder()
                        .consultantId(tempPaymentRequestInfoRequest.getConsultantId())
                        .customerName(tempPaymentRequestInfoRequest.getCustomerName())
                        .customerPhone(tempPaymentRequestInfoRequest.getCustomerPhone())
                        .reservationDate(tempPaymentRequestInfoRequest.getReservationDate())
                        .reservationTime(tempPaymentRequestInfoRequest.getReservationTime())
                        .consultingType(tempPaymentRequestInfoRequest.getConsultingType())
                        .consultingInflowPath(tempPaymentRequestInfoRequest.getConsultingInflowPath())
                        .calcHistoryId(tempPaymentRequestInfoRequest.getCalcHistoryId())
                        .build());

        PaymentHistory paymentHistory = null;

        try{
            paymentHistory = paymentHistoryRepository.saveAndFlush(
                    PaymentHistory.builder()
                            .userId(userUtil.findCurrentUserId())
                            .productPrice(tempPaymentRequestInfoRequest.getProductPrice())
                            .productDiscountPrice(tempPaymentRequestInfoRequest.getProductDiscountPrice())
                            .paymentAmount(tempPaymentRequestInfoRequest.getPaymentAmount())
                            .status(PaymentStatus.READY)
                            .orderId(tempPaymentRequestInfoRequest.getOrderId())
                            .orderName(tempPaymentRequestInfoRequest.getOrderName())
                            .tempRequestedAt(LocalDateTime.now())
                            .productIdList(tempPaymentRequestInfoRequest.getProductId().toString())
                            .productNameList(tempPaymentRequestInfoRequest.getProductName())
                            .consultingReservationId(consultingReservationId)
                            .build());
        }catch(Exception e){
            throw new CustomException(ErrorCode.PAYMENT_REQUEST_OUTPUT_ERROR, "결제요청 데이터 저장 중 오류 발생했어요.");
        }

        return ApiResponse.success(
                TempPaymentRequestInfoResponse.builder()
                        .paymentHistoryId(paymentHistory.getPaymentHistoryId())
                        .paymentStatus(paymentHistory.getStatus())
                        .build());
    }

    // 결제 승인
    public Object confirmPayment(PaymentConfirmRequest paymentConfirmRequest) throws Exception {
        log.info(">> [Service]PaymentService confirmPayment - 결제 승인");
        
        // 결제 승인 유효성 검증
        validationCheckForConfirmPayment(paymentConfirmRequest);

        log.info("결제 승인 : " + paymentConfirmRequest);

        Long paymentHistoryId = paymentConfirmRequest.getPaymentHistoryId();
        String paymentKey = paymentConfirmRequest.getPaymentKey();
        String orderId = paymentConfirmRequest.getOrderId();
        Long paymentAmount = paymentConfirmRequest.getPaymentAmount();

        PaymentHistory paymentHistory = paymentUtil.findPaymentHistory(paymentHistoryId);

        // 결제요청 임시저장 데이터와 결제승인 요청 데이터를 비교하여 맞지 않는 경우
        if(!orderId.equals(paymentHistory.getOrderId()) || !Objects.equals(paymentAmount, paymentHistory.getPaymentAmount())){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_INPUT_ERROR, "주문번호 또는 결제금액이 사전에 입력한 값과 일치하지 않아요.");
        }

        // 결제키(paymentKey)를 결제이력 DB에 저장
        paymentHistory.setPaymentKey(paymentKey);

        try{
            paymentHistory = paymentHistoryRepository.saveAndFlush(paymentHistory);
        }catch(Exception e){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_INPUT_ERROR, "결제승인 이전 데이터 저장 중 오류 발생했어요.");
        }

        // 토스페이먼츠 API는 시크릿 키를 사용자 ID로 사용하고, 비밀번호는 사용하지 않습니다.
        // 비밀번호가 없다는 것을 알리기 위해 시크릿 키 뒤에 콜론을 추가합니다.
        String widgetSecretKey = "test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6";
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodedBytes = encoder.encode((widgetSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        String authorizations = "Basic " + new String(encodedBytes);

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("Authorization", authorizations);

        ResponseEntity<?> response = null;

        try{
            response = paymentsConfirmApi.confirmPayment(
                    headerMap,
                    TossPaymentsConfirmRequest.builder()
                            .paymentKey(paymentConfirmRequest.getPaymentKey())
                            .orderId(paymentConfirmRequest.getOrderId())
                            .amount(paymentConfirmRequest.getPaymentAmount().toString())
                            .build());
        }catch(Exception e){
            log.error("결제 승인 오류 내용 : " + e.getMessage());
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_OUTPUT_ERROR, e.getMessage());
        }

        log.info("confirm payment response");
        log.info(response.toString());

        String jsonString = EMPTY;
        if(response.getBody() != null)  jsonString = response.getBody().toString();
        System.out.println("jsonString : " + jsonString);

        TossPaymentsConfirmResponse tossPaymentsConfirmResponse = (TossPaymentsConfirmResponse) convertJsonToData(jsonString);
        System.out.println("tossPaymentsConfirmResponse : " + tossPaymentsConfirmResponse);

        if(tossPaymentsConfirmResponse == null){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_OUTPUT_ERROR);
        }

        String method = tossPaymentsConfirmResponse.getMethod();
        PaymentStatus paymentStatus = PaymentStatus.valueOf(tossPaymentsConfirmResponse.getStatus());
        LocalDateTime requestedAt = OffsetDateTime.parse(tossPaymentsConfirmResponse.getRequestedAt()).toLocalDateTime();
        LocalDateTime approvedAt = OffsetDateTime.parse(tossPaymentsConfirmResponse.getApprovedAt()).toLocalDateTime();

        paymentHistory.setMethod(method);
        paymentHistory.setStatus(paymentStatus);
        paymentHistory.setRequestedAt(requestedAt);
        paymentHistory.setApprovedAt(approvedAt);

        try{
            paymentHistoryRepository.saveAndFlush(paymentHistory);
        }catch(Exception e){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_INPUT_ERROR, "결제승인 이후 데이터 저장 중 오류 발생했어요.");
        }

        // 결제승인 실패
        if(!PaymentStatus.DONE.equals(paymentStatus)){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_OUTPUT_ERROR, "결제가 승인되지 않았어요.");
        }
        
        // 상담 상태 변경(결제대기 -> 결제완료)
        ConsultingReservationInfo consultingReservationInfo = consultingReservationUtil.findConsultingReservationInfo(paymentHistory.getConsultingReservationId());
        consultingReservationInfo.setConsultingStatus(ConsultingStatus.PAYMENT_COMPLETED);

        try{
            consultingReservationInfoRepository.saveAndFlush(consultingReservationInfo);
        }catch(Exception e){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_INPUT_ERROR, "결제승인 이후 데이터 저장 중 오류 발생했어요.");
        }

        return ApiResponse.success(
                PaymentConfirmResponse.builder()
                        .paymentHistoryId(paymentHistoryId)
                        .paymentStatus(paymentStatus)
                        .requestedAt(requestedAt)
                        .approvedAt(approvedAt)
                        .consultingReservationId(paymentHistory.getConsultingReservationId())
                        .consultingStatus(consultingReservationInfo.getConsultingStatus())
                        .build());
    }

    // 결제목록 조회
    public Object getPaymentList() throws Exception {
        log.info(">> [Service]PaymentService getPaymentList - 결제목록 조회");

        // 완료된 결제 목록(취소 건은 제외)
        List<PaymentHistory> paymentHistoryList = paymentHistoryRepository.findCompleteListByUserId(userUtil.findCurrentUserId());
        List<PaymentListResponse> paymentListResponseList = null;
        
        // 결제 내역이 존재하는 경우
        if(paymentHistoryList != null){
            paymentListResponseList = new ArrayList<>();

            for(PaymentHistory paymentHistory : paymentHistoryList){
                Long paymentHistoryId = null;
                String approvedDatetime = EMPTY;
                Long paymentAmount = null;
                String consultantName = EMPTY;
                String thumbImageUrl = EMPTY;

                ConsultingReservationInfo consultingReservationInfo = consultingReservationUtil.findConsultingReservationInfo(paymentHistory.getConsultingReservationId());
                if(consultingReservationInfo != null){
                    paymentHistoryId = paymentHistory.getPaymentHistoryId();
                    approvedDatetime = paymentHistory.getApprovedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"));
                    paymentAmount = paymentHistory.getPaymentAmount();

                    ConsultantInfo consultantInfo = consultantUtil.findSelectedConsultantInfo(consultingReservationInfo.getConsultantId());
                    if(consultantInfo != null){
                        consultantName = consultantInfo.getConsultantName();
                        thumbImageUrl = consultantInfo.getThumbImageUrl();
                    }
                }

                paymentListResponseList.add(
                        PaymentListResponse.builder()
                                .paymentHistoryId(paymentHistoryId)
                                .approvedDatetime(approvedDatetime)
                                .paymentAmount(paymentAmount)
                                .consultantName(consultantName)
                                .thumbImageUrl(thumbImageUrl)
                                .build());
            }
        }

        return ApiResponse.success(paymentListResponseList);
    }

    // 결제상세 조회
    public Object getPaymentDetail(Long paymentHistoryId) throws Exception {
        log.info(">> [Service]PaymentService getPaymentDetail - 결제상세 조회");

        if(paymentHistoryId == null){
            throw new CustomException(ErrorCode.PAYMENT_DETAIL_INPUT_ERROR);
        }

        String consultantName = EMPTY;
        String thumbImageUrl = EMPTY;

        PaymentHistory paymentHistory = paymentUtil.findPaymentHistory(paymentHistoryId);

        ConsultingReservationInfo consultingReservationInfo = consultingReservationUtil.findConsultingReservationInfo(paymentHistory.getConsultingReservationId());
        if(consultingReservationInfo != null){
            ConsultantInfo consultantInfo = consultantUtil.findSelectedConsultantInfo(consultingReservationInfo.getConsultantId());
            if(consultantInfo != null){
                consultantName = consultantInfo.getConsultantName();
                thumbImageUrl = consultantInfo.getThumbImageUrl();
            }
        }

        return ApiResponse.success(
                PaymentDetailResponse.builder()
                        .paymentHistoryId(paymentHistoryId)
                        .consultantName(consultantName)
                        .thumbImageUrl(thumbImageUrl)
                        .approvedDatetime(paymentHistory.getApprovedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")))
                        .productPrice(paymentHistory.getProductPrice())
                        .productDiscountPrice(paymentHistory.getProductDiscountPrice())
                        .paymentAmount(paymentHistory.getPaymentAmount())
                        .method(paymentHistory.getMethod())
                        .build());
    }

    // 결제 취소(Not For API)
    /*public void cancelPayment(Long consultingReservationId, String cancelReason) throws Exception {
        log.info(">> [Service]PaymentService cancelPayment - 결제 취소(Not For API)");

        PaymentHistory paymentHistory = paymentUtil.findPaymentHistoryByConsultingReservationId(consultingReservationId);
        String paymentKey = paymentHistory.getPaymentKey();

        if(StringUtils.isBlank(paymentKey)){
            throw new CustomException(ErrorCode.PAYMENT_CANCEL_INPUT_ERROR, "해당 상담예약에 사용된 결제정보를 찾지 못했어요.");
        }

        // 토스페이먼츠 API는 시크릿 키를 사용자 ID로 사용하고, 비밀번호는 사용하지 않습니다.
        // 비밀번호가 없다는 것을 알리기 위해 시크릿 키 뒤에 콜론을 추가합니다.
        String widgetSecretKey = "test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6";
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodedBytes = encoder.encode((widgetSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        String authorizations = "Basic " + new String(encodedBytes);

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("Authorization", authorizations);

        ResponseEntity<?> response = null;

        try{
            response = paymentsConfirmApi.cancelPayment(
                    paymentKey,
                    headerMap,
                    TossPaymentsCancelRequest.builder()
                            .cancelReason(cancelReason)
                            .build());
        }catch(Exception e){
            log.error("결제 취소 오류 내용 : " + e.getMessage());
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_OUTPUT_ERROR, e.getMessage());
        }

        log.info("confirm payment response");
        log.info(response.toString());

        String jsonString = EMPTY;
        if(response.getBody() != null)  jsonString = response.getBody().toString();
        System.out.println("jsonString : " + jsonString);

        TossPaymentsCommonResponse tossPaymentsCommonResponse = (TossPaymentsCommonResponse) convertJsonToData(jsonString);
        System.out.println("tossPaymentsCommonResponse : " + tossPaymentsCommonResponse);

        if(tossPaymentsCommonResponse == null){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_OUTPUT_ERROR);
        }

        PaymentStatus paymentStatus = PaymentStatus.valueOf(tossPaymentsCommonResponse.getStatus());

        // 결제승인 실패
        if(!PaymentStatus.CANCELED.equals(paymentStatus)){
            throw new CustomException(ErrorCode.PAYMENT_CANCEL_OUTPUT_ERROR, "결제가 취소되지 않았어요.");
        }

        paymentHistory.setStatus(paymentStatus);

        try{
            paymentHistoryRepository.saveAndFlush(paymentHistory);
        }catch(Exception e){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_INPUT_ERROR, "결제취소 이후 결제상태 변경 중 오류 발생했어요.");
        }
    }*/

    // 결제요청정보 임시저장 유효성 검증
    private void validationCheckForSaveTempPaymentRequestInfo(TempPaymentRequestInfoRequest tempPaymentRequestInfoRequest) {
        if(tempPaymentRequestInfoRequest == null){
            throw new CustomException(ErrorCode.PAYMENT_REQUEST_INPUT_ERROR);
        }

        Long consultantId = tempPaymentRequestInfoRequest.getConsultantId();
        String customerName = tempPaymentRequestInfoRequest.getCustomerName();
        String customerPhone = tempPaymentRequestInfoRequest.getCustomerPhone();
        LocalDate reservationDate = tempPaymentRequestInfoRequest.getReservationDate();
        String reservationTime = tempPaymentRequestInfoRequest.getReservationTime();
        String consultingInflowPath = tempPaymentRequestInfoRequest.getConsultingInflowPath();
        String consultingTypeStr = tempPaymentRequestInfoRequest.getConsultingType();

        if(consultantId == null){
            throw new CustomException(ErrorCode.PAYMENT_REQUEST_INPUT_ERROR, "결제 요청을 위한 상담자ID가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(customerName)){
            throw new CustomException(ErrorCode.PAYMENT_REQUEST_INPUT_ERROR, "결제 요청을 위한 고객명이 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(customerPhone)){
            throw new CustomException(ErrorCode.PAYMENT_REQUEST_INPUT_ERROR, "결제 요청을 위한 고객전화번호가 입력되지 않았습니다.");
        }

        if(reservationDate == null){
            throw new CustomException(ErrorCode.PAYMENT_REQUEST_INPUT_ERROR, "결제 요청을 위한 예약일자가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(reservationTime)){
            throw new CustomException(ErrorCode.PAYMENT_REQUEST_INPUT_ERROR, "결제 요청을 위한 예약시간이 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(consultingInflowPath)){
            throw new CustomException(ErrorCode.PAYMENT_REQUEST_INPUT_ERROR, "결제 요청을 위한 상담유입경로가 입력되지 않았습니다.");
        }else{
            if(!CONSULTING_TYPE_GEN.equals(consultingInflowPath) && !CONSULTING_TYPE_BUY.equals(consultingInflowPath) && !CONSULTING_TYPE_SELL.equals(consultingInflowPath)){
                throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "결제 요청을 위한 상담유입경로 값이 올바르지 않습니다. (00:일반, 01:취득세 계산, 02:양도소득세 계산)");
            }
        }

        if(!StringUtils.isBlank(consultingTypeStr)){
            String[] consultingTypeArr = consultingTypeStr.split(COMMA);
            for (String consultingType : consultingTypeArr) {
                if (!CONSULTING_TYPE_BUY.equals(consultingType) && !CONSULTING_TYPE_SELL.equals(consultingType) && !CONSULTING_TYPE_INHERIT.equals(consultingType) && !CONSULTING_TYPE_PROPERTY.equals(consultingType)) {
                    throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "결제 요청을 위한 상담유형 값이 올바르지 않습니다. (01:취득세, 02:양도소득세, 03:상속세, 04:재산세)");
                }
            }
        }
    }

    // 결제 승인 유효성 검증
    private void validationCheckForConfirmPayment(PaymentConfirmRequest paymentConfirmRequest) {
        if(paymentConfirmRequest == null){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_INPUT_ERROR);
        }

        Long paymentHistoryId = paymentConfirmRequest.getPaymentHistoryId();
        String paymentKey = paymentConfirmRequest.getPaymentKey();
        String orderId = paymentConfirmRequest.getOrderId();
        Long paymentAmount = paymentConfirmRequest.getPaymentAmount();

        if(paymentHistoryId == null){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_INPUT_ERROR, "결제승인을 위한 결제이력ID가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(paymentKey)){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_INPUT_ERROR, "결제승인을 위한 결제키(KEY)가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(orderId)){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_INPUT_ERROR, "결제승인을 위한 주문번호가 입력되지 않았습니다.");
        }

        if(paymentAmount == null || paymentAmount < 0){
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_INPUT_ERROR, "결제승인을 위한 결제금액이 올바르지 않습니다.");
        }
    }

    private Object convertJsonToData(String jsonString) {
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonString, TossPaymentsConfirmResponse.class);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_OUTPUT_ERROR);
        }
    }
}
