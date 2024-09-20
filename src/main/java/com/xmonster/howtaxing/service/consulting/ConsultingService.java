package com.xmonster.howtaxing.service.consulting;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultResponse;
import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultResponse.CalculationBuyOneResult;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultResponse;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultResponse.CalculationSellOneResult;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.consulting.*;
import com.xmonster.howtaxing.dto.consulting.ConsultingAvailableScheduleSearchResponse.ConsultingAvailableDateResponse;
import com.xmonster.howtaxing.dto.consulting.ConsultingAvailableScheduleSearchResponse.ConsultingAvailableTimeResponse;
import com.xmonster.howtaxing.dto.consulting.ConsultingReservationListResponse.ConsultingReservationSimpleResponse;
import com.xmonster.howtaxing.model.*;
import com.xmonster.howtaxing.repository.calculation.*;
import com.xmonster.howtaxing.repository.consulting.ConsultantInfoRepository;
import com.xmonster.howtaxing.repository.consulting.ConsultingReservationInfoRepository;
import com.xmonster.howtaxing.repository.consulting.ConsultingScheduleManagementRepository;
import com.xmonster.howtaxing.type.ConsultingStatus;
import com.xmonster.howtaxing.type.ErrorCode;
import com.xmonster.howtaxing.type.LastModifierType;
import com.xmonster.howtaxing.utils.HouseUtil;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
    private final CalculationBuyResponseHistoryRepository calculationBuyResponseHistoryRepository;
    private final CalculationSellResponseHistoryRepository calculationSellResponseHistoryRepository;
    private final CalculationCommentaryResponseHistoryRepository calculationCommentaryResponseHistoryRepository;

    private final UserUtil userUtil;
    private final HouseUtil houseUtil;

    // 상담가능일정 조회
    public Object getConsultingAvailableSchedule(Long consultantId, String searchType, String searchDate) throws Exception{
        log.info(">> [Service]ConsultingService getConsultingAvailableSchedule - 상담가능일정 조회");

        validationCheckForGetConsultingAvailableSchedule(consultantId, searchType, searchDate);

        log.info("상담가능일정 조회 요청 : " + consultantId + ", " + searchType + ", " + searchDate);

        List<ConsultingAvailableDateResponse> consultingAvailableDateResponseList = null;
        List<ConsultingAvailableTimeResponse> consultingAvailableTimeResponseList = null;

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
                    consultingAvailableTimeResponseList = getReservationAvailableTimeList(consultingScheduleManagement, reservationDate);
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

    // 상담 예약 신청
    public Object applyConsultingReservation(ConsultingReservationApplyRequest consultingReservationApplyRequest) throws Exception {
        log.info(">> [Service]ConsultingService applyConsultingReservation - 상담 예약 신청");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        validationCheckForApplyConsultingReservation(consultingReservationApplyRequest);

        log.info("상담 예약 신청 요청 : " + consultingReservationApplyRequest);

        Long consultantId = consultingReservationApplyRequest.getConsultantId();
        String customerName = consultingReservationApplyRequest.getCustomerName();
        String customerPhone = consultingReservationApplyRequest.getCustomerPhone();
        LocalDate reservationDate = consultingReservationApplyRequest.getReservationDate();
        String reservationTime = consultingReservationApplyRequest.getReservationTime();
        String consultingType = consultingReservationApplyRequest.getConsultingType();
        String consultingInflowPath = consultingReservationApplyRequest.getConsultingInflowPath();
        Long calcHistoryId = consultingReservationApplyRequest.getCalcHistoryId();
        String consultingRequestContent = consultingReservationApplyRequest.getConsultingRequestContent();

        ConsultantInfo consultantInfo = consultantInfoRepository.findByConsultantId(consultantId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "존재하지 않는 상담자ID 입니다."));
        String consultantName = consultantInfo.getConsultantName();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime reservationStartTime = LocalTime.parse(reservationTime, timeFormatter);
        // TODO. 단위를 가져와서 작업 필요
        LocalTime reservationEndTime = reservationStartTime.plusMinutes(30);

        // 요청한 예약일자, 예약시간에 기존 신청된 건이 존재하는지 검증
        long duplicateCheck = consultingReservationInfoRepository.countByReservationDateAndReservationStartTime(reservationDate, reservationStartTime);
        if(duplicateCheck > 0){
            throw new CustomException(ErrorCode.CONSULTING_RESERVATION_DUPLICATED_ERROR);
        }

        // TODO. 고객전화번호 데이터 포맷 검증 필요

        try{
            consultingReservationInfoRepository.saveAndFlush(
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
                            .isCanceled(false)
                            .consultingRequestDatetime(LocalDateTime.now())
                            .lastModifier(LastModifierType.USER)
                            .build());
        }catch(Exception e){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_OUTPUT_ERROR);
        }

        return ApiResponse.success(
                ConsultingReservationApplyResponse.builder()
                        .isApplyComplete(true)
                        .consultantName(consultantName)
                        .reservationDate(reservationDate)
                        .reservationStartTime(reservationStartTime.format(timeFormatter))
                        .reservationEndTime(reservationEndTime.format(timeFormatter))
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

        if(!StringUtils.isBlank(reservationTime)) {
            reservationStartTime = LocalTime.parse(reservationTime, timeFormatter);
            // TODO. 단위를 가져와서 작업 필요
            reservationEndTime = reservationStartTime.plusMinutes(30);

            // 요청한 예약일자, 예약시간에 기존 신청된 건이 존재하는지 검증
            long duplicateCheck = consultingReservationInfoRepository.countByReservationDateAndReservationStartTime(reservationDate, reservationStartTime);
            if(duplicateCheck > 0){
                throw new CustomException(ErrorCode.CONSULTING_RESERVATION_DUPLICATED_ERROR);
            }
        }

        ConsultingReservationInfo consultingReservationInfo = consultingReservationInfoRepository.findByConsultingReservationId(consultingReservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONSULTING_MODIFY_INPUT_ERROR, "존재하지 않는 상담예약ID 입니다."));

        ConsultantInfo consultantInfo = consultantInfoRepository.findByConsultantId(consultingReservationInfo.getConsultantId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONSULTING_MODIFY_INPUT_ERROR, "존재하지 않는 상담자ID 입니다."));
        String consultantName = consultantInfo.getConsultantName();

        if(!findUser.getId().equals(consultingReservationInfo.getUserId())){
            throw new CustomException(ErrorCode.CONSULTING_MODIFY_OUTPUT_ERROR, "본인의 상담 예약 신청 건이 아니기 때문에 변경할 수 없습니다.");
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
    public Object cancelConsultingReservation(Long consultingReservationId) throws Exception {
        log.info(">> [Service]ConsultingService cancelConsultingReservation - 상담 예약 취소");

        if(consultingReservationId == null){
            throw new CustomException(ErrorCode.CONSULTING_CANCEL_INPUT_ERROR, "상담 예약 취소를 위한 상담예약ID가 입력되지 않았습니다.");
        }

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        ConsultingReservationInfo consultingReservationInfo = consultingReservationInfoRepository.findByConsultingReservationId(consultingReservationId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONSULTING_CANCEL_INPUT_ERROR, "존재하지 않는 상담예약ID 입니다."));

        ConsultantInfo consultantInfo = consultantInfoRepository.findByConsultantId(consultingReservationInfo.getConsultantId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONSULTING_CANCEL_INPUT_ERROR, "존재하지 않는 상담자ID 입니다."));
        String consultantName = consultantInfo.getConsultantName();

        if(!findUser.getId().equals(consultingReservationInfo.getUserId())){
            throw new CustomException(ErrorCode.CONSULTING_MODIFY_OUTPUT_ERROR, "본인의 상담 예약 신청 건이 아니기 때문에 취소할 수 없습니다.");
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String reservationStartTime = consultingReservationInfo.getReservationStartTime().format(timeFormatter);
        String reservationEndTime = consultingReservationInfo.getReservationEndTime().format(timeFormatter);

        consultingReservationInfo.setIsCanceled(true);
        consultingReservationInfo.setConsultingStatus(ConsultingStatus.CANCEL);
        consultingReservationInfo.setLastModifier(LastModifierType.USER);

        try{
            consultingReservationInfoRepository.saveAndFlush(consultingReservationInfo);
        }catch (Exception e){
            log.error("상담 예약 update 중 오류가 발생했습니다.");
            throw new CustomException(ErrorCode.CONSULTING_MODIFY_OUTPUT_ERROR);
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

    // 상담 예약 목록 조회
    public Object getConsultingReservationList() throws Exception {
        log.info(">> [Service]ConsultingService getConsultingReservationList - 상담 예약 목록 조회");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        List<ConsultingReservationSimpleResponse> consultingReservationSimpleResponseList = new ArrayList<>();

        List<ConsultingReservationInfo> consultingReservationInfoList = consultingReservationInfoRepository.findByUserIdOrderByReservationDateDescReservationStartTimeDesc(findUser.getId());

        if(consultingReservationInfoList != null && !consultingReservationInfoList.isEmpty()){
            for(ConsultingReservationInfo consultingReservationInfo : consultingReservationInfoList){
                String consultantName = EMPTY;
                ConsultantInfo consultantInfo = consultantInfoRepository.findByConsultantId(consultingReservationInfo.getConsultantId()).orElse(null);
                if(consultantInfo != null){
                    consultantName = consultantInfo.getConsultantName();
                }

                consultingReservationSimpleResponseList.add(
                        ConsultingReservationSimpleResponse.builder()
                                .consultingReservationId(consultingReservationInfo.getConsultingReservationId())
                                .consultantName(consultantName)
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

        if(!findUser.getId().equals(consultingReservationInfo.getUserId())){
            throw new CustomException(ErrorCode.CONSULTING_MODIFY_OUTPUT_ERROR, "본인의 상담 예약 신청 건이 아니기 때문에 취소할 수 없습니다.");
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String reservationStartTime = consultingReservationInfo.getReservationStartTime().format(timeFormatter);
        String reservationEndTime = consultingReservationInfo.getReservationEndTime().format(timeFormatter);

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
                    List<CalculationBuyResponseHistory> calculationBuyResponseHistoryList = calculationBuyResponseHistoryRepository.findByCalcHistoryId(calcHistoryId);

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
                    List<CalculationSellResponseHistory> calculationSellResponseHistoryList = calculationSellResponseHistoryRepository.findByCalcHistoryId(calcHistoryId);

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

        return ApiResponse.success(
                ConsultingReservationDetailResponse.builder()
                        .consultingReservationId(consultingReservationId)
                        .consultantName(consultantName)
                        .consultingType(consultingReservationInfo.getConsultingType())
                        .reservationDate(consultingReservationInfo.getReservationDate())
                        .reservationStartTime(reservationStartTime)
                        .reservationEndTime(reservationEndTime)
                        .consultingStatus(consultingReservationInfo.getConsultingStatus())
                        .consultingInflowPath(consultingReservationInfo.getConsultingInflowPath())
                        .consultingRequestContent(consultingReservationInfo.getConsultingRequestContent())
                        .calculationBuyResultResponse(calculationBuyResultResponse)
                        .calculationSellResultResponse(calculationSellResultResponse)
                        .build());
    }

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

    private void validationCheckForApplyConsultingReservation(ConsultingReservationApplyRequest consultingReservationApplyRequest){
        if(consultingReservationApplyRequest == null) throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR);

        Long consultantId = consultingReservationApplyRequest.getConsultantId();
        String customerName = consultingReservationApplyRequest.getCustomerName();
        String customerPhone = consultingReservationApplyRequest.getCustomerPhone();
        LocalDate reservationDate = consultingReservationApplyRequest.getReservationDate();
        String reservationTime = consultingReservationApplyRequest.getReservationTime();
        String consultingInflowPath = consultingReservationApplyRequest.getConsultingInflowPath();
        String consultingTypeStr = consultingReservationApplyRequest.getConsultingType();

        if(consultantId == null){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 신청을 위한 상담자ID가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(customerName)){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 신청을 위한 고객명이 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(customerPhone)){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 신청을 위한 고객전화번호가 입력되지 않았습니다.");
        }

        if(reservationDate == null){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 신청을 위한 예약일자가 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(reservationTime)){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 신청을 위한 예약시간이 입력되지 않았습니다.");
        }

        if(StringUtils.isBlank(consultingInflowPath)){
            throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 신청을 위한 상담유입경로가 입력되지 않았습니다.");
        }else{
            if(!CONSULTING_TYPE_GEN.equals(consultingInflowPath) && !CONSULTING_TYPE_BUY.equals(consultingInflowPath) && !CONSULTING_TYPE_SELL.equals(consultingInflowPath)){
                throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 신청을 위한 상담유입경로 값이 올바르지 않습니다. (00:일반, 01:취득세 계산, 02:양도소득세 계산)");
            }
        }

        if(!StringUtils.isBlank(consultingTypeStr)){
            String[] consultingTypeArr = consultingTypeStr.split(COMMA);
            for (String consultingType : consultingTypeArr) {
                if (!CONSULTING_TYPE_BUY.equals(consultingType) && !CONSULTING_TYPE_SELL.equals(consultingType) && !CONSULTING_TYPE_INHERIT.equals(consultingType) && !CONSULTING_TYPE_PROPERTY.equals(consultingType)) {
                    throw new CustomException(ErrorCode.CONSULTING_APPLY_INPUT_ERROR, "상담 예약 신청을 위한 상담유형 값이 올바르지 않습니다. (01:취득세, 02:양도소득세, 03:상속세, 04:재산세)");
                }
            }
        }
    }

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

    // 예약 가능 시간 리스트 가져오기
    private List<ConsultingAvailableTimeResponse> getReservationAvailableTimeList(ConsultingScheduleManagement consultingScheduleManagement, LocalDate reservationDate){
        LocalTime reservationAvailableStartTime = consultingScheduleManagement.getReservationAvailableStartTime();
        LocalTime reservationAvailableEndTime = consultingScheduleManagement.getReservationAvailableEndTime();
        int reservationTimeUnit = consultingScheduleManagement.getReservationTimeUnit();
        String reservationUnavailableTime = consultingScheduleManagement.getReservationUnavailableTime();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        List<String> reservationUnavailableTimeList = new ArrayList<>();
        List<ConsultingReservationInfo> consultingReservationInfoList = new ArrayList<>();
        List<ConsultingAvailableTimeResponse> timeList = new ArrayList<>();

        reservationUnavailableTimeList = Arrays.asList(reservationUnavailableTime.split(COMMA));
        consultingReservationInfoList = consultingReservationInfoRepository.findByReservationDate(reservationDate);

        LocalTime compareTime = reservationAvailableStartTime;

        while(compareTime.isBefore(reservationAvailableEndTime)){
            String compareTimeStr = compareTime.format(timeFormatter);
            String reservationStatus = ONE;         // 예약대기

            if(!reservationUnavailableTimeList.isEmpty()){
                for(String unavailableTime : reservationUnavailableTimeList){
                    if(compareTimeStr.equals(unavailableTime)){
                        reservationStatus = THREE;  // 예약불가
                        break;
                    }
                }
            }

            if(consultingReservationInfoList != null && !consultingReservationInfoList.isEmpty()){
                for(ConsultingReservationInfo consultingReservationInfo : consultingReservationInfoList){
                    String reservedTimeStr = consultingReservationInfo.getReservationStartTime().format(timeFormatter);
                    if(compareTimeStr.equals(reservedTimeStr)){
                        reservationStatus = TWO;    // 예약완료
                        break;
                    }
                }
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
}
