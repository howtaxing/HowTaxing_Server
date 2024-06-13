package com.xmonster.howtaxing.controller.calculation;

import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultRequest;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultRequest;
import com.xmonster.howtaxing.service.calculation.CalculationBuyService;
import com.xmonster.howtaxing.service.calculation.CalculationSellService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CalculationController {
    private final CalculationBuyService calculationBuyService;
    private final CalculationSellService calculationSellService;

    // 취득세 계산 결과 조회
    @PostMapping("/calculation/buyResult")
    public Object getCalculationBuyResult(@RequestBody CalculationBuyResultRequest calculationBuyResultRequest) throws Exception {
        log.info(">> [Controller]CalculationController getCalculationBuyResult - 취득세 계산 결과 조회");
        return calculationBuyService.getCalculationBuyResult(calculationBuyResultRequest);
    }

    // 양도소득세 계산 결과 조회
    @PostMapping("/calculation/sellResult")
    public Object getCalculationSellResult(@RequestBody CalculationSellResultRequest calculationSellResultRequest) throws Exception {
        log.info(">> [Controller]CalculationController getCalculationBuyResult - 취득세 계산 결과 조회");
        return calculationSellService.getCalculationSellResult(calculationSellResultRequest);
    }
}
