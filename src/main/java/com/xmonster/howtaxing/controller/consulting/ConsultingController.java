package com.xmonster.howtaxing.controller.consulting;

import com.xmonster.howtaxing.dto.consulting.*;
import com.xmonster.howtaxing.service.consulting.ConsultingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ConsultingController {
    private final ConsultingService consultingService;

    // 상담자 정보 목록 조회
    @GetMapping("/consulting/consultantList")
    public Object getConsultantInfoList() throws Exception {
        log.info(">> [Controller]ConsultingController getConsultantInfoList - 상담자 정보 목록 조회");
        return consultingService.getConsultantInfoList();
    }

    // 상담자 정보 상세 조회
    @GetMapping("/consulting/consultantDetail")
    public Object getConsultantInfoDetail(@RequestParam Long consultantId) throws Exception {
        log.info(">> [Controller]ConsultingController getConsultantInfoDetail - 상담자 정보 상세 조회");
        return consultingService.getConsultantInfoDetail(consultantId);
    }

    // 상담 가능 일정 조회
    @GetMapping("/consulting/availableSchedule")
    public Object getConsultingAvailableSchedule(@RequestParam Long consultantId, @RequestParam String searchType, @RequestParam(defaultValue = "") String searchDate) throws Exception {
        log.info(">> [Controller]ConsultingController getConsultingAvailableSchedule - 상담 가능 일정 조회");
        return consultingService.getConsultingAvailableSchedule(consultantId, searchType, searchDate);
    }

    // 상담 예약 신청
    @PostMapping("/consulting/reservationApply")
    public Object applyConsultingReservation(@RequestBody ConsultingReservationApplyRequest consultingReservationApplyRequest) throws Exception {
        log.info(">> [Controller]ConsultingController applyConsultingReservation - 상담 예약 신청");
        return consultingService.applyConsultingReservation(consultingReservationApplyRequest);
    }

    // 무료 상담 예약 신청
    @PostMapping("/consulting/reservationApplyForFree")
    public Object applyConsultingReservationForFree(@RequestBody ConsultingReservationApplyForFreeRequest consultingReservationApplyForFreeRequest) throws Exception {
        log.info(">> [Controller]ConsultingController applyConsultingReservationForFree - 무료 상담 예약 신청");
        return consultingService.applyConsultingReservationForFree(consultingReservationApplyForFreeRequest);
    }

    // 상담 예약 변경
    @PostMapping("/consulting/reservationModify")
    public Object modifyConsultingReservation(@RequestBody ConsultingReservationModifyRequest consultingReservationModifyRequest) throws Exception {
        log.info(">> [Controller]ConsultingController modifyConsultingReservation - 상담 예약 변경");
        return consultingService.modifyConsultingReservation(consultingReservationModifyRequest);
    }

    // 상담 예약 취소
    @PostMapping("/consulting/reservationCancel")
    public Object cancelConsultingReservation(@RequestBody ConsultingReservationCancelRequest consultingReservationCancelRequest) throws Exception {
        log.info(">> [Controller]ConsultingController cancelConsultingReservation - 상담 예약 취소");
        return consultingService.cancelConsultingReservation(consultingReservationCancelRequest);
    }

    // 상담 예약 목록 조회
    @GetMapping("/consulting/reservationList")
    public Object getConsultingReservationList() throws Exception {
        log.info(">> [Controller]ConsultingController getConsultingReservationList - 상담 예약 목록 조회");
        return consultingService.getConsultingReservationList();
    }

    // 상담 예약 상세 조회
    @GetMapping("/consulting/reservationDetail")
    public Object getConsultingReservationDetail(@RequestParam Long consultingReservationId) throws Exception {
        log.info(">> [Controller]ConsultingController getConsultingReservationList - 상담 예약 상세 조회");
        return consultingService.getConsultingReservationDetail(consultingReservationId);
    }

    // 상담 예약 가능여부 조회
    @PostMapping("/consulting/reservationAvailable")
    public Object getIsReservationAvailable(@RequestBody ConsultingReservationAvailableRequest consultingReservationAvailableRequest) throws Exception {
        log.info(">> [Controller]ConsultingController getIsReservationAvailable - 상담 예약 가능여부 조회");
        return consultingService.getIsReservationAvailable(consultingReservationAvailableRequest);
    }
}
