package com.xmonster.howtaxing.controller.consulting;

import com.xmonster.howtaxing.service.consulting.ConsultingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ConsultingController {
    private final ConsultingService consultingService;

    // 상담 가능 일정 조회
    @GetMapping("/consulting/availableSchedule")
    public Object getConsultingAvailableSchedule(@RequestParam Long consultantId, @RequestParam String searchType, @RequestParam LocalDate searchDate) throws Exception {
        log.info(">> [Controller]ConsultingController getConsultingAvailableSchedule - 상담 가능 일정 조회");
        return consultingService.getConsultingAvailableSchedule(consultantId, searchType, searchDate);
    }
}
