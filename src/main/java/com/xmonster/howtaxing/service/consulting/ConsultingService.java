package com.xmonster.howtaxing.service.consulting;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.consulting.ConsultingAvailableScheduleSearchResponse;
import com.xmonster.howtaxing.dto.consulting.ConsultingAvailableScheduleSearchResponse.ConsultingAvailableDateResponse;
import com.xmonster.howtaxing.dto.consulting.ConsultingAvailableScheduleSearchResponse.ConsultingAvailableTimeResponse;
import com.xmonster.howtaxing.model.ConsultingScheduleId;
import com.xmonster.howtaxing.model.ConsultingScheduleManagement;
import com.xmonster.howtaxing.repository.consulting.ConsultingScheduleManagementRepository;
import com.xmonster.howtaxing.type.ErrorCode;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ConsultingService {

    private final ConsultingScheduleManagementRepository consultingScheduleManagementRepository;

    private final UserUtil userUtil;

    // 상담가능일정 조회
    public Object getConsultingAvailableSchedule(Long consultantId, String searchType, LocalDate searchDate){
        log.info(">> [Service]ConsultingService getConsultingAvailableSchedule - 상담가능일정 조회");

        validationCheckForGetConsultingAvailableSchedule(consultantId, searchType, searchDate);

        log.info("상담가능일정 조회 요청 : " + consultantId + ", " + searchType + ", " + searchDate);

        List<ConsultingAvailableDateResponse> consultingAvailableDateResponseList = null;
        List<ConsultingAvailableTimeResponse> consultingAvailableTimeResponseList = null;

        long checkedConsultantId = 1;   // 상담자가 늘어나기 전까지는 1로 고정(이민정음 세무사 1명)

        // 상담가능일자 조회
        if(ONE.equals(searchType)){
            consultingAvailableDateResponseList = new ArrayList<>();
            List<ConsultingScheduleManagement> consultingScheduleManagementList = consultingScheduleManagementRepository.findByConsultantIdAfterToday(checkedConsultantId, LocalDate.now());

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
            consultingAvailableTimeResponseList = new ArrayList<>();
            ConsultingScheduleManagement consultingScheduleManagement = consultingScheduleManagementRepository.findByConsultingScheduleId(
                    ConsultingScheduleId.builder()
                            .consultantId(checkedConsultantId)
                            .reservationDate(searchDate)
                            .build());

            if(consultingScheduleManagement != null){
                if(consultingScheduleManagement.getIsReservationAvailable()){
                    consultingAvailableTimeResponseList = getReservationAvailableTimeList(consultingScheduleManagement);
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

    private void validationCheckForGetConsultingAvailableSchedule(Long consultantId, String searchType, LocalDate searchDate){
        if(StringUtils.isBlank(searchType)){
            throw new CustomException(ErrorCode.CONSULTING_SCHEDULE_INPUT_ERROR, "상담가능일정 조회를 위한 조회구분이 입력되지 않았습니다.");
        }else{
            if(TWO.equals(searchType)){
                if(searchDate != null){
                    throw new CustomException(ErrorCode.CONSULTING_SCHEDULE_INPUT_ERROR, "상담가능일정 조회를 위한 조회일자가 입력되지 않았거나 값이 올바르지 않습니다.");
                }
            }else{
                if(!ONE.equals(searchType)){
                    throw new CustomException(ErrorCode.CONSULTING_SCHEDULE_INPUT_ERROR, "상담가능일정 조회를 위한 조회구분 입력값이 올바르지 않습니다.");
                }
            }
        }
    }

    private List<ConsultingAvailableTimeResponse> getReservationAvailableTimeList(ConsultingScheduleManagement consultingScheduleManagement){
        String reservationAvailableStartTime = consultingScheduleManagement.getReservationAvailableStartTime();
        String reservationAvailableEndTime = consultingScheduleManagement.getReservationAvailableEndTime();
        int reservationTimeUnit = consultingScheduleManagement.getReservationTimeUnit();
        String reservationUnavailableTime = consultingScheduleManagement.getReservationUnavailableTime();

        List<String> reservationUnavailableTimeList = new ArrayList<>();
        List<ConsultingAvailableTimeResponse> timeList = new ArrayList<>();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime startTime = LocalTime.parse(reservationAvailableStartTime, timeFormatter);
        LocalTime endTime = LocalTime.parse(reservationAvailableEndTime, timeFormatter);

        reservationUnavailableTimeList = Arrays.asList(reservationUnavailableTime.split(COMMA));

        LocalTime compareTime = startTime;

        while(compareTime.isBefore(endTime)){
            String compareTimeStr = compareTime.format(timeFormatter);
            String reservationStatus = ONE; // 예약대기

            for(String unavailableTime : reservationUnavailableTimeList){
                if(compareTimeStr.equals(unavailableTime)){
                    reservationStatus = THREE;  // 예약불가
                }
            }

            // TODO : 예약완료 케이스도 추가해야함

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
