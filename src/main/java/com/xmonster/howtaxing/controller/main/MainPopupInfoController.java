package com.xmonster.howtaxing.controller.main;

import com.xmonster.howtaxing.service.main.MainPopupInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MainPopupInfoController {

    private final MainPopupInfoService mainPopupInfoService;

    // 메인팝업정보 조회
    @GetMapping("/main/mainPopup")
    public Object getMainPopupInfo() throws Exception {
        log.info(">> [Controller]MainPopupInfoController getMainPopupInfo - 메인팝업정보 조회");
        return mainPopupInfoService.getMainPopupInfo();
    }
}