package com.xmonster.howtaxing.service.consulting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.calculation.CalculationBuyHouseResponse;
import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultResponse;
import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultResponse.CalculationBuyOneResult;
import com.xmonster.howtaxing.dto.calculation.CalculationSellHouseResponse;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultResponse;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultResponse.CalculationSellOneResult;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.consulting.*;
import com.xmonster.howtaxing.dto.consulting.ConsultingAvailableScheduleSearchResponse.ConsultingAvailableDateResponse;
import com.xmonster.howtaxing.dto.consulting.ConsultingAvailableScheduleSearchResponse.ConsultingAvailableTimeResponse;
import com.xmonster.howtaxing.dto.consulting.ConsultingReservationListResponse.ConsultingReservationSimpleResponse;
import com.xmonster.howtaxing.dto.payment.TossPaymentsCancelRequest;
import com.xmonster.howtaxing.dto.payment.TossPaymentsCommonResponse;
import com.xmonster.howtaxing.dto.payment.TossPaymentsConfirmResponse;
import com.xmonster.howtaxing.feign.tosspayments.PaymentsConfirmApi;
import com.xmonster.howtaxing.model.*;
import com.xmonster.howtaxing.repository.calculation.*;
import com.xmonster.howtaxing.repository.consulting.ConsultantInfoRepository;
import com.xmonster.howtaxing.repository.consulting.ConsultingReservationInfoRepository;
import com.xmonster.howtaxing.repository.consulting.ConsultingScheduleManagementRepository;
import com.xmonster.howtaxing.repository.payment.PaymentHistoryRepository;
import com.xmonster.howtaxing.service.payment.PaymentService;
import com.xmonster.howtaxing.type.ConsultingStatus;
import com.xmonster.howtaxing.type.ErrorCode;
import com.xmonster.howtaxing.type.LastModifierType;
import com.xmonster.howtaxing.type.PaymentStatus;
import com.xmonster.howtaxing.utils.ConsultantUtil;
import com.xmonster.howtaxing.utils.ConsultingReservationUtil;
import com.xmonster.howtaxing.utils.PaymentUtil;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ConsultingService {
    private final ConsultingScheduleManagementRepository consultingScheduleManagementRepository;
    private final ConsultingReservationInfoRepository consultingReservationInfoRepository;
    private final ConsultantInfoRepository consultantInfoRepository;
    private final CalculationHistoryRepository calculationHistoryRepository;
    private final CalculationBuyRequestHistoryRepository calculationBuyRequestHistoryRepository;
    private final CalculationBuyResponseHistoryRepository calculationBuyResponseHistoryRepository;
    private final CalculationSellRequestHistoryRepository calculationSellRequestHistoryRepository;
    private final CalculationSellResponseHistoryRepository calculationSellResponseHistoryRepository;
    private final CalculationCommentaryResponseHistoryRepository calculationCommentaryResponseHistoryRepository;
    private final CalculationOwnHouseHistoryRepository calculationOwnHouseHistoryRepository;
    private final CalculationOwnHouseHistoryDetailRepository calculationOwnHouseHistoryDetailRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    private final UserUtil userUtil;
    private final ConsultingReservationUtil consultingReservationUtil;
    private final ConsultantUtil consultantUtil;
    private final PaymentUtil paymentUtil;

    private final PaymentsConfirmApi paymentsConfirmApi;

    // 상담자 정보 목록 조회
    public Object getConsultantInfoList() throws Exception {
        log.info(">> [Service]ConsultingService getConsultantInfoList - 상담자 정보 목록 조회");

        //List<ConsultantInfo> consultantInfoList = consultantInfoRepository.findAll(Sort.by(Sort.Direction.ASC, "consultantId"));
        List<ConsultantInfo> consultantInfoList = consultantInfoRepository.findAvailableConsultantList();
        List<ConsultantListResponse> consultantListResponseList = null;

        if(consultantInfoList != null){
            consultantListResponseList = new ArrayList<>();

            for(ConsultantInfo consultantInfo : consultantInfoList){
                consultantListResponseList.add(
                        ConsultantListResponse.builder()
                                .consultantId(consultantInfo.getConsultantId())
                                .consultantName(consultantInfo.getConsultantName())
                                .jobTitle(consultantInfo.getJobTitle())
                                .company(consultantInfo.getCompany())
                                .location(consultantInfo.getLocation())
                                .consultantIntroduction(consultantInfo.getConsultantIntroduction())
                                .thumbImageUrl(consultantInfo.getThumbImageUrl())
                                .build());
            }
        }

        return ApiResponse.success(consultantListResponseList);
    }

    // 상담자 정보 상세 조회
    public Object getConsultantInfoDetail(Long consultantId) throws Exception {
        log.info(">> [Service]ConsultingService getConsultantInfoList - 상담자 정보 상세 조회");

        if(consultantId == null){
            throw new CustomException(ErrorCode.CONSULTING_CONSULTANT_INPUT_ERROR, "상담자 정보 상세 조회를 위한 상담자ID 정보가 입력되지 않았습니다");
        }

        ConsultantInfo consultantInfo = consultantInfoRepository.findByConsultantId(consultantId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "존재하지 않는 상담자ID 입니다."));

        List<String> specialtyResponseList = null;
        List<String> majorExperienceResponseList = null;

        if(StringUtils.isNotBlank(consultantInfo.getSpecialtyContents())){
            specialtyResponseList = Arrays.asList(consultantInfo.getSpecialtyContents().split(COMMA));
        }

        if(StringUtils.isNotBlank(consultantInfo.getMajorExperienceContents())){
            majorExperienceResponseList = Arrays.asList(consultantInfo.getMajorExperienceContents().split(COMMA));
        }

        return ApiResponse.success(
                ConsultantDetailResponse.builder()
                        .consultantId(consultantInfo.getConsultantId())
                        .consultantName(consultantInfo.getConsultantName())
                        .jobTitle(consultantInfo.getJobTitle())
                        .company(consultantInfo.getCompany())
                        .qualification(consultantInfo.getQualification())
                        .location(consultantInfo.getLocation())
                        .consultingType(consultantInfo.getConsultingType())
                        .consultantIntroduction(consultantInfo.getConsultantIntroduction())
                        .specialtyList(specialtyResponseList)
                        .majorExperienceList(majorExperienceResponseList)
                        .profileImageUrl(consultantInfo.getProfileImageUrl())
                        .build());
    }

    // 상담가능일정 조회
    public Object getConsultingAvailableSchedule(Long consultantId, String searchType, String searchDate) throws Exception {
        log.info(">> [Service]ConsultingService getConsultingAvailableSchedule - 상담가능일정 조회");

        validationCheckForGetConsultingAvailableSchedule(consultantId, searchType, searchDate);

        log.info("상담가능일정 조회 요청 : " + consultantId + ", " + searchType + ", " + searchDate);

        List<ConsultingAvailableDateResponse> consultingAvailableDateResponseList = null;
        List<ConsultingAvailableTimeResponse> consultingAvailableTimeResponseList = null;

        // TODO. 상담자가 늘어나면 선택한 값을 세팅하도록 변경
        long checkedConsultantId = 1;   // 상담자가 늘어나기 전까지는 1로 고정(이민정음 세무사 1명)

        // 상담가능일자 조회
        if(ONE.equals(searchType)){
            consultingAvailableDateResponseList = new ArrayList<>();
            List<ConsultingScheduleManagement> consultingScheduleManagementList = new ArrayList<>();

            try{
                consultingScheduleManagementList = consultingScheduleManagementRepository.findByConsultantIdAfterToday(checkedConsultantId, LocalDate.now());
            }catch (Exception e){
                throw new CustomException(ErrorCode.CONSULTING_SCHEDULE_OUTPUT_ERROR);
            }

            if(consultingScheduleManagementList != null && !consultingScheduleManagementList.isEmpty()){
                for(ConsultingScheduleManagement consultingScheduleManagement : consultingScheduleManagementList){
                    consultingAvailableDateResponseList.add(
                            ConsultingAvailableDateResponse.builder()
                                    .consultingDate(consultingScheduleManagement.getConsultingScheduleId().getReservationDate())
                                    .isReservationAvailable(consultingScheduleManagement.getIsReservationAvailable())
                                    .build());
                }
            }
        }
        // 상담가능시간 조회
        else if(TWO.equals(searchType)){
            LocalDate reservationDate = LocalDate.parse(searchDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            consultingAvailableTimeResponseList = new ArrayList<>();
            ConsultingScheduleManagement consultingScheduleManagement = consultingScheduleManagementRepository.findByConsultingScheduleId(
                    ConsultingScheduleId.builder()
                            .consultantId(checkedConsultantId)
                            .reservationDate(reservationDate)
                            .build());

            if(consultingScheduleManagement != null){
                if(consultingScheduleManagement.getIsReservationAvailable()){
                    consultingAvailableTimeResponseList = getReservationAvailableTimeList(checkedConsultantId, consultingScheduleManagement, reservationDate);
                }
            }
        }

        return ApiResponse.success(
                ConsultingAvailableScheduleSearchResponse.builder()
                        .consultantId(checkedConsultantId)
                        .searchType(searchType)
                        .dateList(consultingAvailableDateResponseList)
                        .timeList(consultingAvailableTimeResponseList)
                        .build());
    }

    // 상담 예약 가능여부 조회
    public Object getIsReservationAvailable(ConsultingReservationAvailableRequest consultingReservationAvailableRequest) throws Exception {
        log.info(">> [Service]ConsultingService getIsReservationAvailable - 상담 예약 가능여부 조회");

        validationCheckForGetIsReservationAvailable(consultingReservationAvailableRequest);

        log.info("consultingReservationAvailableRequest : " + consultingReservationAvailableRequest);

        Long consultantId = consultingReservationAvailableRequest.getConsultantId();
        LocalDate reservationDate = consultingReservationAvailableRequest.getReservationDate();
        String reservationTime = consultingReservationAvailableRequest.getReservationTime();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime reservationStartTime = LocalTime.parse(reservationTime, timeFormatter);

        checkReservationAvailable(consultantId, reservationDate, reservationStartTime);

        log.info("상담 예약 가능 확인");
        return ApiResponse.success(Map.of("result", "선택한 날짜와 시간으로 상담 예약이 가능해요."));
    }

    // 상담예약 생성(결제 시점, Not for API)
    public Long createConsultingReservation(ConsultingReservationCreateRequest consultingReservationCreateRequest) throws Exception {
        log.info(">> [Service]ConsultingService createConsultingReservationInfo - 상담예약정보 생성");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        validationCheckForCreateConsultingReservation(consultingReservationCreateRequest);

        log.info("상담예약 정보 생성 요청 : " + consultingReservationCreateRequest);

        Long consultantId = consultingReservationCreateRequest.getConsultantId();
        String customerName = consultingReservationCreateRequest.getCustomerName();
        String customerPhone = consultingReservationCreateRequest.getCustomerPhone();
        LocalDate reservationDate = consultingReservationCreateRequest.getReservationDate();
        String reservationTime = consultingReservationCreateRequest.getReservationTime();
        String consultingType = consultingReservationCreateRequest.getConsultingType();
        String consultingInflowPath = consultingReservationCreateRequest.getConsultingInflowPath();
        Long paymentAmount = consultingReservationCreateRequest.getPaymentAmount();
        Long calcHistoryId = consultingReservationCreateRequest.getCalcHistoryId();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime reservationStartTime = LocalTime.parse(reservationTime, timeFormatter);
        // TODO. 단위를 가져와서 작업 필요
        LocalTime reservationEndTime = reservationStartTime.plusMinutes(30);

        checkReservationAvailable(consultantId, reservationDate, reservationStartTime);

        /*
        // 본인이 당일 기존 예약 신청한 건이 존재하는지 검증
        long alreadyReservationCheck = consultingReservationInfoRepository.countByUserIdAndReservationDate(findUser.getId(), reservationDate);
        if(alreadyReservationCheck > 0){
            throw new CustomException(ErrorCode.CONSULTING_RESERVATION_ALREADY_ERROR);
        }

        // 요청한 예약일자, 예약시간에 기존 신청된 건이 존재하는지 검증
        long duplicateCheck = consultingReservationInfoRepository.countByReservationDateAndReservationStartTime(consultantId, reservationDate, reservationStartTime);
        if(duplicateCheck > 0){
            throw new CustomException(ErrorCode.CONSULTING_RESERVATION_DUPLICATED_ERROR);
        }
        */

        // TODO. 고객전화번호 데이터 포맷 검증 필요

        ConsultingReservationInfo consultingReservationInfo = null;

        try{
            consultingReservationInfo = consultingReservationInfoRepository.saveAndFlush(
                    ConsultingReservationInfo.builder()
                            .consultantId(consultantId)
                            .userId(findUser.getId())
                            .calcHistoryId(calcHistoryId)
                            .consultingType(consultingType)
                            .reservationDate(reservationDate)
                            .reservationStartTime(reservationStartTime)
                            .reservationEndTime(reservationEndTime)
                            .customerName(customerName)
                            .customerPhone(customerPhone)
                            .consultingInflowPath(consultingInflowPath)
                            .consultingStatus(ConsultingStatus.PAYMENT_READY)
                            .paymentAmount(paymentAmount)
                            .isCanceled(false)
                            .lastModifier(LastModifierType.USER)
                            .build());
        }catch(Exception e){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_OUTPUT_ERROR, "상담예약정보 데이터 저장 중 오류 발생했어요.");
        }

        return consultingReservationInfo.getConsultingReservationId();
    }

    // 상담 예약 신청
    public Object applyConsultingReservation(ConsultingReservationApplyRequest consultingReservationApplyRequest) throws Exception {
        log.info(">> [Service]ConsultingService applyConsultingReservation - 상담 예약 신청");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        validationCheckForApplyConsultingReservation(consultingReservationApplyRequest);

        log.info("상담 예약 신청 요청 : " + consultingReservationApplyRequest);

        Long consultingReservationId = consultingReservationApplyRequest.getConsultingReservationId();
        String consultingType = consultingReservationApplyRequest.getConsultingType();
        String consultingRequestContent = consultingReservationApplyRequest.getConsultingRequestContent();

        ConsultingReservationInfo consultingReservationInfo = consultingReservationUtil.findConsultingReservationInfo(consultingReservationId);
        Long consultantId = consultingReservationInfo.getConsultantId();
        String consultantName = consultantUtil.findConsultantName(consultantId);

        consultingReservationInfo.setConsultingType(consultingType);
        consultingReservationInfo.setConsultingRequestContent(consultingRequestContent);
        consultingReservationInfo.setConsultingStatus(ConsultingStatus.WAITING);
        consultingReservationInfo.setConsultingRequestDatetime(LocalDateTime.now());

        try{
            consultingReservationInfoRepository.saveAndFlush(consultingReservationInfo);
        }catch(Exception e){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_OUTPUT_ERROR);
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return ApiResponse.success(
                ConsultingReservationApplyResponse.builder()
                        .isApplyComplete(true)
                        .consultantName(consultantName)
                        .reservationDate(consultingReservationInfo.getReservationDate())
                        .reservationStartTime(consultingReservationInfo.getReservationStartTime().format(timeFormatter))
                        .reservationEndTime(consultingReservationInfo.getReservationEndTime().format(timeFormatter))
                        .build());
    }

    // 무료상담예약 신청
    public Object applyConsultingReservationForFree(ConsultingReservationApplyForFreeRequest consultingReservationApplyForFreeRequest) throws Exception {
        log.info(">> [Service]ConsultingService applyConsultingReservation - 무료상담예약 신청");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        validationCheckForApplyConsultingReservationForFree(consultingReservationApplyForFreeRequest);

        log.info("무료상담예약 신청 요청 : " + consultingReservationApplyForFreeRequest);

        Long consultantId = consultingReservationApplyForFreeRequest.getConsultantId();
        String customerName = consultingReservationApplyForFreeRequest.getCustomerName();
        String customerPhone = consultingReservationApplyForFreeRequest.getCustomerPhone();
        LocalDate reservationDate = consultingReservationApplyForFreeRequest.getReservationDate();
        String reservationTime = consultingReservationApplyForFreeRequest.getReservationTime();
        String consultingInflowPath = consultingReservationApplyForFreeRequest.getConsultingInflowPath();
        Long calcHistoryId = consultingReservationApplyForFreeRequest.getCalcHistoryId();
        String consultingType = consultingReservationApplyForFreeRequest.getConsultingType();
        String consultingRequestContent = consultingReservationApplyForFreeRequest.getConsultingRequestContent();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime reservationStartTime = LocalTime.parse(reservationTime, timeFormatter);
        // TODO. 단위를 가져와서 작업 필요
        LocalTime reservationEndTime = reservationStartTime.plusMinutes(30);

        checkReservationAvailable(consultantId, reservationDate, reservationStartTime);

        String consultantName = consultantUtil.findConsultantName(consultantId);

        ConsultingReservationInfo consultingReservationInfo = null;

        try{
            consultingReservationInfo = consultingReservationInfoRepository.saveAndFlush(
                    ConsultingReservationInfo.builder()
                            .consultantId(consultantId)
                            .userId(findUser.getId())
                            .calcHistoryId(calcHistoryId)
                            .consultingType(consultingType)
                            .reservationDate(reservationDate)
                            .reservationStartTime(reservationStartTime)
                            .reservationEndTime(reservationEndTime)
                            .customerName(customerName)
                            .customerPhone(customerPhone)
                            .consultingInflowPath(consultingInflowPath)
                            .consultingRequestContent(consultingRequestContent)
                            .consultingStatus(ConsultingStatus.WAITING)
                            .consultingRequestDatetime(LocalDateTime.now())
                            .paymentAmount(0L)
                            .isCanceled(false)
                            .lastModifier(LastModifierType.USER)
                            .build());
        }catch(Exception e){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_OUTPUT_ERROR, "상담예약정보 데이터 저장 중 오류 발생했어요.");
        }

        return ApiResponse.success(
                ConsultingReservationApplyResponse.builder()
                        .isApplyComplete(true)
                        .consultantName(consultantName)
                        .reservationDate(consultingReservationInfo.getReservationDate())
                        .reservationStartTime(consultingReservationInfo.getReservationStartTime().format(timeFormatter))
                        .reservationEndTime(consultingReservationInfo.getReservationEndTime().format(timeFormatter))
                        .build());
    }

    // 상담 예약 변경
    public Object modifyConsultingReservation(ConsultingReservationModifyRequest consultingReservationModifyRequest) throws Exception {
        log.info(">> [Service]ConsultingService modifyConsultingReservation - 상담 예약 변경");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        validationCheckForModifyConsultingReservation(consultingReservationModifyRequest);

        Long consultingReservationId = consultingReservationModifyRequest.getConsultingReservationId();
        String customerName = consultingReservationModifyRequest.getCustomerName();
        String customerPhone = consultingReservationModifyRequest.getCustomerPhone();
        LocalDate reservationDate = consultingReservationModifyRequest.getReservationDate();
        String reservationTime = consultingReservationModifyRequest.getReservationTime();
        String consultingType = consultingReservationModifyRequest.getConsultingType();
        String consultingRequestContent = consultingReservationModifyRequest.getConsultingRequestContent();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime reservationStartTime = null;
        LocalTime reservationEndTime = null;

        ConsultingReservationInfo consultingReservationInfo = consultingReservationInfoRepository.findByConsultingReservationId(consultingReservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONSULTING_MODIFY_INPUT_ERROR, "존재하지 않는 상담예약ID 입니다."));

        ConsultantInfo consultantInfo = consultantInfoRepository.findByConsultantId(consultingReservationInfo.getConsultantId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONSULTING_MODIFY_INPUT_ERROR, "존재하지 않는 상담자ID 입니다."));
        String consultantName = consultantInfo.getConsultantName();

        if(!findUser.getId().equals(consultingReservationInfo.getUserId())){
            throw new CustomException(ErrorCode.CONSULTING_MODIFY_OUTPUT_ERROR, "본인의 상담 예약 신청 건이 아니기 때문에 변경할 수 없습니다.");
        }

        if(!StringUtils.isBlank(reservationTime)) {
            reservationStartTime = LocalTime.parse(reservationTime, timeFormatter);
            // TODO. 단위를 가져와서 작업 필요
            reservationEndTime = reservationStartTime.plusMinutes(30);

            // 요청한 예약일자, 예약시간에 기존 신청된 건이 존재하는지 검증
            long duplicateCheck = consultingReservationInfoRepository.countByReservationDateAndReservationStartTime(consultingReservationInfo.getConsultantId(), reservationDate, reservationStartTime);
            if(duplicateCheck > 0){
                throw new CustomException(ErrorCode.CONSULTING_RESERVATION_DUPLICATED_ERROR);
            }
        }

        if(!StringUtils.isBlank(customerName)) consultingReservationInfo.setCustomerName(customerName);
        if(!StringUtils.isBlank(customerPhone)) consultingReservationInfo.setCustomerPhone(customerPhone);
        if(reservationDate != null) consultingReservationInfo.setReservationDate(reservationDate);
        if(!StringUtils.isBlank(reservationTime)){
            consultingReservationInfo.setReservationStartTime(reservationStartTime);
            consultingReservationInfo.setReservationEndTime(reservationEndTime);
        }
        if(!StringUtils.isBlank(consultingType)) consultingReservationInfo.setConsultingType(consultingType);
        if(!StringUtils.isBlank(consultingRequestContent)) consultingReservationInfo.setConsultingRequestContent(consultingRequestContent);

        consultingReservationInfo.setLastModifier(LastModifierType.USER);
        
        try{
            consultingReservationInfoRepository.saveAndFlush(consultingReservationInfo);
        }catch (Exception e){
            log.error("상담 예약 update 중 오류가 발생했습니다.");
            throw new CustomException(ErrorCode.CONSULTING_MODIFY_OUTPUT_ERROR);
        }

        return ApiResponse.success(
                ConsultingReservationModifyResponse.builder()
                        .isModifyComplete(true)
                        .consultantName(consultantName)
                        .reservationDate(consultingReservationInfo.getReservationDate())
                        .reservationStartTime(consultingReservationInfo.getReservationStartTime().format(timeFormatter))
                        .reservationEndTime(consultingReservationInfo.getReservationEndTime().format(timeFormatter))
                        .build());
    }

    // 상담 예약 취소
    public Object cancelConsultingReservation(ConsultingReservationCancelRequest consultingReservationCancelRequest) throws Exception {
        log.info(">> [Service]ConsultingService cancelConsultingReservation - 상담 예약 취소");

        // 상담 예약 취소 유효성 검증
        validationCheckForCancelConsultingReservation(consultingReservationCancelRequest);

        Long consultingReservationId = consultingReservationCancelRequest.getConsultingReservationId();
        String cancelReason = consultingReservationCancelRequest.getCancelReason();

        ConsultingReservationInfo consultingReservationInfo = consultingReservationUtil.findConsultingReservationInfo(consultingReservationId);
        String consultantName = consultantUtil.findConsultantName(consultingReservationInfo.getConsultantId());

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();
        if(!findUser.getId().equals(consultingReservationInfo.getUserId())){
            throw new CustomException(ErrorCode.CONSULTING_MODIFY_OUTPUT_ERROR, "본인의 상담 예약 신청 건이 아니기 때문에 취소할 수 없습니다.");
        }

        //paymentService.cancelPayment(consultingReservationId, cancelReason);    // 결제 취소

        PaymentHistory paymentHistory = paymentUtil.findPaymentHistoryByConsultingReservationId(consultingReservationId);
        
        // 해당 상담 건에 결제정보가 존재하는 경우 결제취소(환불) 처리
        if(paymentHistory != null){
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
                paymentHistoryRepository.save(paymentHistory);
            }catch(Exception e){
                throw new CustomException(ErrorCode.PAYMENT_CONFIRM_INPUT_ERROR, "결제취소 이후 결제상태 변경 중 오류 발생했어요.");
            }
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String reservationStartTime = consultingReservationInfo.getReservationStartTime().format(timeFormatter);
        String reservationEndTime = consultingReservationInfo.getReservationEndTime().format(timeFormatter);

        consultingReservationInfo.setIsCanceled(true);
        consultingReservationInfo.setConsultingStatus(ConsultingStatus.CANCEL);
        consultingReservationInfo.setConsultingCancelDatetime(LocalDateTime.now());
        consultingReservationInfo.setLastModifier(LastModifierType.USER);

        try{
            consultingReservationInfoRepository.save(consultingReservationInfo);    // 상담예약 취소 정보 업데이트
        }catch (Exception e){
            log.error("상담 예약 취소 중 오류가 발생했습니다.");
            throw new CustomException(ErrorCode.CONSULTING_CANCEL_OUTPUT_ERROR);
        }

        return ApiResponse.success(
                ConsultingReservationCancelResponse.builder()
                        .isCancelComplete(true)
                        .consultantName(consultantName)
                        .reservationDate(consultingReservationInfo.getReservationDate())
                        .reservationStartTime(reservationStartTime)
                        .reservationEndTime(reservationEndTime)
                        .build());
    }

    private Object convertJsonToData(String jsonString) {
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonString, TossPaymentsCommonResponse.class);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_OUTPUT_ERROR);
        }
    }

    // 상담 예약 목록 조회
    public Object getConsultingReservationList() throws Exception {
        log.info(">> [Service]ConsultingService getConsultingReservationList - 상담 예약 목록 조회");

        List<ConsultingReservationSimpleResponse> consultingReservationSimpleResponseList = new ArrayList<>();

        //List<ConsultingReservationInfo> consultingReservationInfoList = consultingReservationInfoRepository.findByUserIdOrderByReservationDateDescReservationStartTimeDesc(userUtil.findCurrentUserId());
        List<ConsultingReservationInfo> consultingReservationInfoList = consultingReservationInfoRepository.findUserReservationInfoList(userUtil.findCurrentUserId());

        if(consultingReservationInfoList != null && !consultingReservationInfoList.isEmpty()){
            for(ConsultingReservationInfo consultingReservationInfo : consultingReservationInfoList){
                String consultantName = null;
                String thumbImageUrl = null;
                ConsultantInfo consultantInfo = consultantInfoRepository.findByConsultantId(consultingReservationInfo.getConsultantId()).orElse(null);
                if(consultantInfo != null){
                    consultantName = consultantInfo.getConsultantName();
                    thumbImageUrl = consultantInfo.getThumbImageUrl();
                }

                consultingReservationSimpleResponseList.add(
                        ConsultingReservationSimpleResponse.builder()
                                .consultingReservationId(consultingReservationInfo.getConsultingReservationId())
                                .consultantName(consultantName)
                                .thumbImageUrl(thumbImageUrl)
                                .consultingType(consultingReservationInfo.getConsultingType())
                                .reservationDate(consultingReservationInfo.getReservationDate())
                                .reservationStartTime(consultingReservationInfo.getReservationStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                                .reservationEndTime(consultingReservationInfo.getReservationEndTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                                .consultingStatus(consultingReservationInfo.getConsultingStatus())
                                .build());
            }
        }

        return ApiResponse.success(
                ConsultingReservationListResponse.builder()
                        .listCnt(consultingReservationSimpleResponseList.size())
                        .list(consultingReservationSimpleResponseList)
                        .build());
    }

    // 상담 예약 상세 조회
    public Object getConsultingReservationDetail(Long consultingReservationId) throws Exception {
        log.info(">> [Service]ConsultingService getConsultingReservationList - 상담 예약 상세 조회");

        if(consultingReservationId == null){
            throw new CustomException(ErrorCode.CONSULTING_CANCEL_INPUT_ERROR, "상담 예약 상세조회를 위한 상담예약ID가 입력되지 않았습니다.");
        }

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        ConsultingReservationInfo consultingReservationInfo = consultingReservationInfoRepository.findByConsultingReservationId(consultingReservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONSULTING_CANCEL_INPUT_ERROR, "존재하지 않는 상담예약ID 입니다."));

        ConsultantInfo consultantInfo = consultantInfoRepository.findByConsultantId(consultingReservationInfo.getConsultantId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONSULTING_CANCEL_INPUT_ERROR, "존재하지 않는 상담자ID 입니다."));
        String consultantName = consultantInfo.getConsultantName();
        String profileImageUrl = consultantInfo.getProfileImageUrl();

        if(!findUser.getId().equals(consultingReservationInfo.getUserId())){
            throw new CustomException(ErrorCode.CONSULTING_MODIFY_OUTPUT_ERROR, "본인의 상담 예약 신청 건이 아니기 때문에 취소할 수 없습니다.");
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String reservationStartTime = consultingReservationInfo.getReservationStartTime().format(timeFormatter);
        String reservationEndTime = consultingReservationInfo.getReservationEndTime().format(timeFormatter);

        LocalDateTime paymentCompleteDatetime = consultingReservationInfo.getPaymentCompleteDatetime();
        LocalDateTime consultingRequestDatetime = consultingReservationInfo.getConsultingRequestDatetime();
        LocalDateTime consultingCancelDatetime = consultingReservationInfo.getConsultingCancelDatetime();
        LocalDateTime consultingStartDatetime = consultingReservationInfo.getConsultingStartDatetime();
        LocalDateTime consultingEndDatetime = consultingReservationInfo.getConsultingEndDatetime();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
        String paymentCompleteDatetimeStr = (paymentCompleteDatetime != null) ? paymentCompleteDatetime.format(dateTimeFormatter) : null;
        String consultingRequestDatetimeStr = (consultingRequestDatetime != null) ? consultingRequestDatetime.format(dateTimeFormatter) : null;
        String consultingCancelDatetimeStr = (consultingCancelDatetime != null) ? consultingReservationInfo.getConsultingCancelDatetime().format(dateTimeFormatter) : null;
        String consultingStartDatetimeStr = (consultingStartDatetime != null) ? consultingReservationInfo.getConsultingStartDatetime().format(dateTimeFormatter) : null;
        String consultingEndDatetimeStr = (consultingEndDatetime != null) ? consultingReservationInfo.getConsultingEndDatetime().format(dateTimeFormatter) : null;

        CalculationBuyHouseResponse calculationBuyHouseResponse = null;
        CalculationSellHouseResponse calculationSellHouseResponse = null;

        CalculationBuyResultResponse calculationBuyResultResponse = null;
        CalculationSellResultResponse calculationSellResultResponse = null;

        // 계산이력ID
        Long calcHistoryId = consultingReservationInfo.getCalcHistoryId();

        // 계산이력ID 값이 존재하면 응답값에 계산 결과 세팅
        if(calcHistoryId != null){
            CalculationHistory calculationHistory = null;
            Long userId = null;
            String calcType = EMPTY;

            calculationHistory = calculationHistoryRepository.findByCalcHistoryId(calcHistoryId);

            if(calculationHistory != null){
                userId = calculationHistory.getUserId();
                calcType = calculationHistory.getCalcType();

                int listCnt = 0;
                int commentaryListCnt = 0;
                List<String> commentaryList = new ArrayList<>();
                String calculationResultTextData = EMPTY;

                List<CalculationCommentaryResponseHistory> commentaryResponseHistoryList = calculationCommentaryResponseHistoryRepository.findByCalcHistoryId(calcHistoryId);

                if(commentaryResponseHistoryList != null){
                    commentaryListCnt = commentaryResponseHistoryList.size();

                    for(CalculationCommentaryResponseHistory calculationCommentaryResponseHistory : commentaryResponseHistoryList){
                        commentaryList.add(calculationCommentaryResponseHistory.getCommentaryContent());
                    }
                }

                // 취득세 계산
                if(CALC_TYPE_BUY.equals(calcType)){
                    List<CalculationBuyOneResult> list = new ArrayList<>();
                    CalculationBuyRequestHistory calculationBuyRequestHistory = calculationBuyRequestHistoryRepository.findByCalcHistoryId(calcHistoryId);
                    List<CalculationBuyResponseHistory> calculationBuyResponseHistoryList = calculationBuyResponseHistoryRepository.findByCalcHistoryId(calcHistoryId);

                    if(calculationBuyRequestHistory != null){
                        calculationBuyHouseResponse =
                                CalculationBuyHouseResponse.builder()
                                        .houseType(calculationBuyRequestHistory.getHouseType())
                                        .houseName(calculationBuyRequestHistory.getHouseName())
                                        .detailAdr(calculationBuyRequestHistory.getDetailAdr())
                                        .contractDate(calculationBuyRequestHistory.getContractDate())
                                        .balanceDate(calculationBuyRequestHistory.getBalanceDate())
                                        .buyDate(calculationBuyRequestHistory.getBuyDate())
                                        .buyPrice(calculationBuyRequestHistory.getBuyPrice())
                                        .pubLandPrice(calculationBuyRequestHistory.getPubLandPrice())
                                        .isPubLandPriceOver100Mil(calculationBuyRequestHistory.getIsPubLandPriceOver100Mil())
                                        .roadAddr(calculationBuyRequestHistory.getRoadAddr())
                                        .area(calculationBuyRequestHistory.getArea())
                                        .isAreaOver85(calculationBuyRequestHistory.getIsAreaOver85())
                                        .isDestruction(calculationBuyRequestHistory.getIsDestruction())
                                        .ownerCnt(calculationBuyRequestHistory.getOwnerCnt())
                                        .userProportion(calculationBuyRequestHistory.getUserProportion())
                                        .isMoveInRight(calculationBuyRequestHistory.getIsMoveInRight())
                                        .build();
                    }

                    if(calculationBuyResponseHistoryList != null){
                        listCnt = calculationBuyResponseHistoryList.size();

                        for(CalculationBuyResponseHistory calculationBuyResponseHistory : calculationBuyResponseHistoryList){
                            list.add(
                                    CalculationBuyOneResult.builder()
                                            .buyPrice(calculationBuyResponseHistory.getBuyPrice())
                                            .buyTaxRate(calculationBuyResponseHistory.getBuyTaxRate())
                                            .buyTaxPrice(calculationBuyResponseHistory.getBuyTaxPrice())
                                            .eduTaxRate(calculationBuyResponseHistory.getEduTaxRate())
                                            .eduTaxPrice(calculationBuyResponseHistory.getEduTaxPrice())
                                            .eduDiscountPrice(calculationBuyResponseHistory.getEduDiscountPrice())
                                            .agrTaxRate(calculationBuyResponseHistory.getAgrTaxRate())
                                            .agrTaxPrice(calculationBuyResponseHistory.getAgrTaxPrice())
                                            .totalTaxPrice(calculationBuyResponseHistory.getTotalTaxPrice())
                                            .build());
                        }
                    }

                    calculationBuyResultResponse = CalculationBuyResultResponse.builder()
                            .listCnt(listCnt)
                            .list(list)
                            .commentaryListCnt(commentaryListCnt)
                            .commentaryList(commentaryList)
                            .calcHistoryId(calcHistoryId)
                            .build();
                }
                else if(CALC_TYPE_SELL.equals(calcType)){
                    List<CalculationSellOneResult> list = new ArrayList<>();
                    CalculationSellRequestHistory calculationSellRequestHistory = calculationSellRequestHistoryRepository.findByCalcHistoryId(calcHistoryId);
                    List<CalculationSellResponseHistory> calculationSellResponseHistoryList = calculationSellResponseHistoryRepository.findByCalcHistoryId(calcHistoryId);

                    if(calculationSellRequestHistory != null){
                        List<CalculationOwnHouseHistory> calculationOwnHouseHistoryList = calculationOwnHouseHistoryRepository.findByCalcHistoryId(calcHistoryId);
                        if(calculationOwnHouseHistoryList != null && !calculationOwnHouseHistoryList.isEmpty()){
                            long ownHouseHistoryId = calculationOwnHouseHistoryList.get(0).getOwnHouseHistoryId();
                            long sellHouseId = calculationSellRequestHistory.getSellHouseId();
                            List<CalculationOwnHouseHistoryDetail> calculationOwnHouseHistoryDetailList = calculationOwnHouseHistoryDetailRepository.findByOwnHouseHistoryIdAndHouseId(ownHouseHistoryId, sellHouseId);

                            if(calculationOwnHouseHistoryDetailList != null && !calculationOwnHouseHistoryDetailList.isEmpty()){
                                CalculationOwnHouseHistoryDetail calculationOwnHouseHistoryDetail = calculationOwnHouseHistoryDetailList.get(0);
                                boolean isPubLandPriceOver100Mil = calculationOwnHouseHistoryDetail.getPubLandPrice() > ONE_HND_MIL;
                                boolean isAreaOver85 = calculationOwnHouseHistoryDetail.getArea().doubleValue() > AREA_85;

                                calculationSellHouseResponse =
                                        CalculationSellHouseResponse.builder()
                                                .houseType(calculationOwnHouseHistoryDetail.getHouseType())
                                                .houseName(calculationOwnHouseHistoryDetail.getHouseName())
                                                .detailAdr(calculationOwnHouseHistoryDetail.getDetailAdr())
                                                .contractDate(calculationOwnHouseHistoryDetail.getContractDate())
                                                .buyDate(calculationOwnHouseHistoryDetail.getBuyDate())
                                                .buyPrice(calculationOwnHouseHistoryDetail.getBuyPrice())
                                                .pubLandPrice(calculationOwnHouseHistoryDetail.getPubLandPrice())
                                                .isPubLandPriceOver100Mil(isPubLandPriceOver100Mil)
                                                .roadAddr(calculationOwnHouseHistoryDetail.getRoadAddr())
                                                .area(calculationOwnHouseHistoryDetail.getArea().doubleValue())
                                                .isAreaOver85(isAreaOver85)
                                                .isDestruction(calculationOwnHouseHistoryDetail.getIsDestruction())
                                                .ownerCnt(calculationOwnHouseHistoryDetail.getOwnerCnt())
                                                .userProportion(calculationOwnHouseHistoryDetail.getUserProportion())
                                                .isMoveInRight(calculationOwnHouseHistoryDetail.getIsMoveInRight())
                                                .build();
                            }
                        }
                    }

                    if(calculationSellResponseHistoryList != null){
                        listCnt = calculationSellResponseHistoryList.size();

                        for(CalculationSellResponseHistory calculationSellResponseHistory : calculationSellResponseHistoryList){
                            list.add(
                                    CalculationSellOneResult.builder()
                                            .buyPrice(calculationSellResponseHistory.getBuyPrice())
                                            .buyDate(calculationSellResponseHistory.getBuyDate())
                                            .sellPrice(calculationSellResponseHistory.getSellPrice())
                                            .sellDate(calculationSellResponseHistory.getSellDate())
                                            .necExpensePrice(calculationSellResponseHistory.getNecExpensePrice())
                                            .sellProfitPrice(calculationSellResponseHistory.getSellProfitPrice())
                                            .retentionPeriod(calculationSellResponseHistory.getRetentionPeriod())
                                            .nonTaxablePrice(calculationSellResponseHistory.getNonTaxablePrice())
                                            .taxablePrice(calculationSellResponseHistory.getTaxablePrice())
                                            .longDeductionPrice(calculationSellResponseHistory.getLongDeductionPrice())
                                            .sellIncomePrice(calculationSellResponseHistory.getSellIncomePrice())
                                            .basicDeductionPrice(calculationSellResponseHistory.getBasicDeductionPrice())
                                            .taxableStdPrice(calculationSellResponseHistory.getTaxableStdPrice())
                                            .sellTaxRate(calculationSellResponseHistory.getSellTaxRate())
                                            .progDeductionPrice(calculationSellResponseHistory.getProgDeductionPrice())
                                            .sellTaxPrice(calculationSellResponseHistory.getSellTaxPrice())
                                            .localTaxPrice(calculationSellResponseHistory.getLocalTaxPrice())
                                            .totalTaxPrice(calculationSellResponseHistory.getTotalTaxPrice())
                                            .build());
                        }

                        calculationSellResultResponse = CalculationSellResultResponse.builder()
                                .listCnt(listCnt)
                                .list(list)
                                .commentaryListCnt(commentaryListCnt)
                                .commentaryList(commentaryList)
                                .calcHistoryId(calcHistoryId)
                                .build();
                    }else{
                        log.info("양도소득세 계산 응답 이력 없음");
                    }
                }
            }
        }

        ConsultingReservationDetailResponse consultingReservationDetailResponse =
                ConsultingReservationDetailResponse.builder()
                        .consultingReservationId(consultingReservationId)
                        .consultantName(consultantName)
                        .profileImageUrl(profileImageUrl)
                        .consultingType(consultingReservationInfo.getConsultingType())
                        .reservationDate(consultingReservationInfo.getReservationDate())
                        .reservationStartTime(reservationStartTime)
                        .reservationEndTime(reservationEndTime)
                        .consultingStatus(consultingReservationInfo.getConsultingStatus())
                        .consultingInflowPath(consultingReservationInfo.getConsultingInflowPath())
                        .paymentAmount(consultingReservationInfo.getPaymentAmount())
                        .consultingRequestContent(consultingReservationInfo.getConsultingRequestContent())
                        .paymentCompleteDatetime(paymentCompleteDatetimeStr)
                        .consultingRequestDatetime(consultingRequestDatetimeStr)
                        .consultingCancelDatetime(consultingCancelDatetimeStr)
                        .consultingStartDatetime(consultingStartDatetimeStr)
                        .consultingEndDatetime(consultingEndDatetimeStr)
                        .calculationBuyHouseResponse(calculationBuyHouseResponse)
                        .calculationSellHouseResponse(calculationSellHouseResponse)
                        .calculationBuyResultResponse(calculationBuyResultResponse)
                        .calculationSellResultResponse(calculationSellResultResponse)
                        .build();

        log.info("consultingReservationDetailResponse : " + consultingReservationDetailResponse);

        return ApiResponse.success(consultingReservationDetailResponse);
    }

    // 상담가능일정 조회 유효성 검증
    private void validationCheckForGetConsultingAvailableSchedule(Long consultantId, String searchType, String searchDate){
        if(StringUtils.isBlank(searchType)){
            throw new CustomException(ErrorCode.CONSULTING_SCHEDULE_INPUT_ERROR, "상담가능일정 조회를 위한 조회구분이 입력되지 않았습니다.");
        }else{
            if(TWO.equals(searchType)){
                if(StringUtils.isBlank(searchDate)){
                    throw new CustomException(ErrorCode.CONSULTING_SCHEDULE_INPUT_ERROR, "상담가능일정 조회를 위한 조회일자가 입력되지 않았거나 값이 올바르지 않습니다.");
                }
            }else{
                if(!ONE.equals(searchType)){
                    throw new CustomException(ErrorCode.CONSULTING_SCHEDULE_INPUT_ERROR, "상담가능일정 조회를 위한 조회구분 입력값이 올바르지 않습니다.");
                }
            }
        }
    }

    // 상담 예약 가능여부 조회 유효성 검증
    private void validationCheckForGetIsReservationAvailable(ConsultingReservationAvailableRequest consultingReservationAvailableRequest){
        if(consultingReservationAvailableRequest == null){
            throw new CustomException(ErrorCode.CONSULTING_AVAILABLE_INPUT_ERROR);
        }

        Long consultantId = consultingReservationAvailableRequest.getConsultantId();
        LocalDate reservationDate = consultingReservationAvailableRequest.getReservationDate();
        String reservationTime = consultingReservationAvailableRequest.getReservationTime();

        if(consultantId == null){
            throw new CustomException(ErrorCode.CONSULTING_AVAILABLE_INPUT_ERROR, "상담 예약 가능여부 조회를 위한 상담자ID가 입력되지 않았습니다.");
        }

        if(reservationDate == null){
            throw new CustomException(ErrorCode.CONSULTING_AVAILABLE_INPUT_ERROR, "상담 예약 가능여부 조회를 위한 예약일자가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(reservationTime)){
            throw new CustomException(ErrorCode.CONSULTING_AVAILABLE_INPUT_ERROR, "상담 예약 가능여부 조회를 위한 예약시간이 입력되지 않았습니다.");
        }
    }

    // 상담예약생성(결제 시점) 유효성 검증
    private void validationCheckForCreateConsultingReservation(ConsultingReservationCreateRequest consultingReservationCreateRequest){
        if(consultingReservationCreateRequest == null) throw new CustomException(ErrorCode.CONSULTING_CREATE_INPUT_ERROR);

        Long consultantId = consultingReservationCreateRequest.getConsultantId();
        String customerName = consultingReservationCreateRequest.getCustomerName();
        String customerPhone = consultingReservationCreateRequest.getCustomerPhone();
        LocalDate reservationDate = consultingReservationCreateRequest.getReservationDate();
        String reservationTime = consultingReservationCreateRequest.getReservationTime();
        String consultingInflowPath = consultingReservationCreateRequest.getConsultingInflowPath();
        String consultingTypeStr = consultingReservationCreateRequest.getConsultingType();

        if(consultantId == null){
            throw new CustomException(ErrorCode.CONSULTING_CREATE_INPUT_ERROR, "상담예약정보 생성을 위한 상담자ID가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(customerName)){
            throw new CustomException(ErrorCode.CONSULTING_CREATE_INPUT_ERROR, "상담예약정보 생성을 위한 고객명이 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(customerPhone)){
            throw new CustomException(ErrorCode.CONSULTING_CREATE_INPUT_ERROR, "상담예약정보 생성을 위한 고객전화번호가 입력되지 않았습니다.");
        }

        if(reservationDate == null){
            throw new CustomException(ErrorCode.CONSULTING_CREATE_INPUT_ERROR, "상담예약정보 생성을 위한 예약일자가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(reservationTime)){
            throw new CustomException(ErrorCode.CONSULTING_CREATE_INPUT_ERROR, "상담예약정보 생성을 위한 예약시간이 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(consultingInflowPath)){
            throw new CustomException(ErrorCode.CONSULTING_CREATE_INPUT_ERROR, "상담예약정보 생성을 위한 상담유입경로가 입력되지 않았습니다.");
        }else{
            if(!CONSULTING_TYPE_GEN.equals(consultingInflowPath) && !CONSULTING_TYPE_BUY.equals(consultingInflowPath) && !CONSULTING_TYPE_SELL.equals(consultingInflowPath)){
                throw new CustomException(ErrorCode.CONSULTING_CREATE_INPUT_ERROR, "상담예약정보 생성을 위한 상담유입경로 값이 올바르지 않습니다. (00:일반, 01:취득세 계산, 02:양도소득세 계산)");
            }
        }

        if(!StringUtils.isBlank(consultingTypeStr)){
            String[] consultingTypeArr = consultingTypeStr.split(COMMA);
            for (String consultingType : consultingTypeArr) {
                if (!CONSULTING_TYPE_BUY.equals(consultingType) && !CONSULTING_TYPE_SELL.equals(consultingType) && !CONSULTING_TYPE_INHERIT.equals(consultingType) && !CONSULTING_TYPE_PROPERTY.equals(consultingType)) {
                    throw new CustomException(ErrorCode.CONSULTING_CREATE_INPUT_ERROR, "상담예약정보 생성을 위한 상담유형 값이 올바르지 않습니다. (01:취득세, 02:양도소득세, 03:상속세, 04:재산세)");
                }
            }
        }
    }

    // 상담 예약 신청 유효성 검증
    private void validationCheckForApplyConsultingReservation(ConsultingReservationApplyRequest consultingReservationApplyRequest){
        if(consultingReservationApplyRequest == null){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR);
        }

        Long consultingReservationId = consultingReservationApplyRequest.getConsultingReservationId();
        String consultingTypeStr = consultingReservationApplyRequest.getConsultingType();
        String consultingRequestContent = consultingReservationApplyRequest.getConsultingRequestContent();

        if(consultingReservationId == null){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 신청을 위한 상담예약 ID가 입력되지 않았습니다.");
        }

        if(!StringUtils.isBlank(consultingTypeStr)){
            String[] consultingTypeArr = consultingTypeStr.split(COMMA);
            for (String consultingType : consultingTypeArr) {
                if (!CONSULTING_TYPE_BUY.equals(consultingType) && !CONSULTING_TYPE_SELL.equals(consultingType) && !CONSULTING_TYPE_INHERIT.equals(consultingType) && !CONSULTING_TYPE_PROPERTY.equals(consultingType)) {
                    throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 신청을 위한 상담유형 값이 올바르지 않습니다. (01:취득세, 02:양도소득세, 03:상속세, 04:재산세)");
                }
            }
        }

        if(StringUtils.isBlank(consultingRequestContent)){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 신청을 위한 상담요청내용이 입력되지 않았습니다.");
        }
    }

    // 무료 상담 예약 신청 유효성 검증
    private void validationCheckForApplyConsultingReservationForFree(ConsultingReservationApplyForFreeRequest consultingReservationApplyRequestForFree){
        if(consultingReservationApplyRequestForFree == null) throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR);

        Long consultantId = consultingReservationApplyRequestForFree.getConsultantId();
        String customerName = consultingReservationApplyRequestForFree.getCustomerName();
        String customerPhone = consultingReservationApplyRequestForFree.getCustomerPhone();
        LocalDate reservationDate = consultingReservationApplyRequestForFree.getReservationDate();
        String reservationTime = consultingReservationApplyRequestForFree.getReservationTime();
        String consultingInflowPath = consultingReservationApplyRequestForFree.getConsultingInflowPath();
        String consultingTypeStr = consultingReservationApplyRequestForFree.getConsultingType();
        String consultingRequestContent = consultingReservationApplyRequestForFree.getConsultingRequestContent();

        if(consultantId == null){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "무료상담예약 신청을 위한 상담자ID가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(customerName)){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "무료상담예약 신청을 위한 고객명이 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(customerPhone)){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "무료상담예약 신청을 위한 고객전화번호가 입력되지 않았습니다.");
        }

        if(reservationDate == null){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "무료상담예약 신청을 위한 예약일자가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(reservationTime)){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "무료상담예약 신청을 위한 예약시간이 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(consultingInflowPath)){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "무료상담예약 신청을 위한 상담유입경로가 입력되지 않았습니다.");
        }else{
            if(!CONSULTING_TYPE_GEN.equals(consultingInflowPath) && !CONSULTING_TYPE_BUY.equals(consultingInflowPath) && !CONSULTING_TYPE_SELL.equals(consultingInflowPath)){
                throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "무료상담예약 신청을 위한 상담유입경로 값이 올바르지 않습니다. (00:일반, 01:취득세 계산, 02:양도소득세 계산)");
            }
        }

        if(!StringUtils.isBlank(consultingTypeStr)){
            String[] consultingTypeArr = consultingTypeStr.split(COMMA);
            for (String consultingType : consultingTypeArr) {
                if (!CONSULTING_TYPE_BUY.equals(consultingType) && !CONSULTING_TYPE_SELL.equals(consultingType) && !CONSULTING_TYPE_INHERIT.equals(consultingType) && !CONSULTING_TYPE_PROPERTY.equals(consultingType)) {
                    throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "무료상담예약 신청을 위한 상담유형 값이 올바르지 않습니다. (01:취득세, 02:양도소득세, 03:상속세, 04:재산세)");
                }
            }
        }

        if(StringUtils.isBlank(consultingRequestContent)){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "무료상담예약 신청을 위한 상담요청내용이 입력되지 않았습니다.");
        }
    }

    // 상담 예약 변경 유효성 검증
    private void validationCheckForModifyConsultingReservation(ConsultingReservationModifyRequest consultingReservationModifyRequest){
        if(consultingReservationModifyRequest == null) throw new CustomException(ErrorCode.CONSULTING_MODIFY_INPUT_ERROR);

        Long consultingReservationId = consultingReservationModifyRequest.getConsultingReservationId();
        String consultingTypeStr = consultingReservationModifyRequest.getConsultingType();

        if(consultingReservationId == null){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 변경을 위한 상담예약ID가 입력되지 않았습니다.");
        }

        if(!StringUtils.isBlank(consultingTypeStr)){
            String[] consultingTypeArr = consultingTypeStr.split(COMMA);
            for (String consultingType : consultingTypeArr) {
                if (!CONSULTING_TYPE_BUY.equals(consultingType) && !CONSULTING_TYPE_SELL.equals(consultingType) && !CONSULTING_TYPE_INHERIT.equals(consultingType) && !CONSULTING_TYPE_PROPERTY.equals(consultingType)) {
                    throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 변경을 위한 상담유형 값이 올바르지 않습니다. (01:취득세, 02:양도소득세, 03:상속세, 04:재산세)");
                }
            }
        }
    }

    // 상담 예약 취소 유효성 검증
    private void validationCheckForCancelConsultingReservation(ConsultingReservationCancelRequest consultingReservationCancelRequest){
        if(consultingReservationCancelRequest == null){
            throw new CustomException(ErrorCode.CONSULTING_CANCEL_INPUT_ERROR);
        }

        Long consultingReservationId = consultingReservationCancelRequest.getConsultingReservationId();
        String cancelReason = consultingReservationCancelRequest.getCancelReason();

        if(consultingReservationId == null){
            throw new CustomException(ErrorCode.CONSULTING_CANCEL_INPUT_ERROR, "상담 취소를 위한 상담예약ID가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(cancelReason)){
            throw new CustomException(ErrorCode.CONSULTING_CANCEL_INPUT_ERROR, "상담 취소를 위한 취소 사유가 입력되지 않았습니다.");
        }
    }

    // 예약 가능 시간 리스트 가져오기
    private List<ConsultingAvailableTimeResponse> getReservationAvailableTimeList(Long consultantId, ConsultingScheduleManagement consultingScheduleManagement, LocalDate reservationDate){
        LocalTime reservationAvailableStartTime = consultingScheduleManagement.getReservationAvailableStartTime();
        LocalTime reservationAvailableEndTime = consultingScheduleManagement.getReservationAvailableEndTime();
        int reservationTimeUnit = consultingScheduleManagement.getReservationTimeUnit();
        String reservationUnavailableTime = consultingScheduleManagement.getReservationUnavailableTime();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        List<String> reservationUnavailableTimeList = new ArrayList<>();
        List<ConsultingReservationInfo> consultingReservationInfoList = new ArrayList<>();
        List<ConsultingAvailableTimeResponse> timeList = new ArrayList<>();

        reservationUnavailableTimeList = Arrays.asList(reservationUnavailableTime.split(COMMA));
        consultingReservationInfoList = consultingReservationInfoRepository.findByReservationDate(consultantId, reservationDate);

        LocalTime compareTime = reservationAvailableStartTime;

        while(compareTime.isBefore(reservationAvailableEndTime.plusMinutes(reservationTimeUnit))){
            String compareTimeStr = compareTime.format(timeFormatter);
            String reservationStatus = null;

            // 현재일시 이전의 예약일시는 모두 예약불가 처리
            if(reservationDate.isEqual(LocalDate.now())){
                if(compareTime.isBefore(LocalTime.now())){
                    reservationStatus = THREE;
                }
            }else if(reservationDate.isBefore(LocalDate.now())){
                reservationStatus = THREE;
            }


            // 예약 불가시간 목록을 가져와 해당 시간에 예약불가 처리
            if(StringUtils.isBlank(reservationStatus)){
                if(!reservationUnavailableTimeList.isEmpty()){
                    for(String unavailableTime : reservationUnavailableTimeList){
                        if(compareTimeStr.equals(unavailableTime)){
                            reservationStatus = THREE;  // 예약불가
                            break;
                        }
                    }
                }
            }

            // 예약 목록을 가져와 해당 시간에 예약완료 처리
            if(StringUtils.isBlank(reservationStatus)){
                if(consultingReservationInfoList != null && !consultingReservationInfoList.isEmpty()){
                    for(ConsultingReservationInfo consultingReservationInfo : consultingReservationInfoList){
                        String reservedTimeStr = consultingReservationInfo.getReservationStartTime().format(timeFormatter);
                        if(compareTimeStr.equals(reservedTimeStr)){
                            reservationStatus = TWO;    // 예약완료
                            break;
                        }
                    }
                }
            }

            // 어떤 조건에도 해당되지 않은 시간은 예약대기 처리
            if(StringUtils.isBlank(reservationStatus)){
                reservationStatus = ONE;                // 예약대기
            }

            timeList.add(
                    ConsultingAvailableTimeResponse.builder()
                            .consultingTime(compareTimeStr)
                            .consultingTimeUnit(reservationTimeUnit)
                            .reservationStatus(reservationStatus)
                            .build());

            compareTime = compareTime.plusMinutes(reservationTimeUnit);
        }

        return timeList;
    }

    // 상담 예약 가능 여부 체크
    private void checkReservationAvailable(long consultantId, LocalDate reservationDate, LocalTime reservationStartTime){
        // 본인이 당일 기존 예약 신청한 건이 존재하는지 검증
        long alreadyReservationCheck = consultingReservationInfoRepository.countByUserIdAndReservationDate(userUtil.findCurrentUserId(), reservationDate);
        if(alreadyReservationCheck > 0){
            throw new CustomException(ErrorCode.CONSULTING_RESERVATION_ALREADY_ERROR);
        }

        // 요청한 예약일자, 예약시간에 기존 신청된 건이 존재하는지 검증
        long duplicateCheck = consultingReservationInfoRepository.countByReservationDateAndReservationStartTime(consultantId, reservationDate, reservationStartTime);
        if(duplicateCheck > 0){
            throw new CustomException(ErrorCode.CONSULTING_RESERVATION_DUPLICATED_ERROR);
        }
    }
}
