package com.xmonster.howtaxing.service.main;

import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.main.MainPopupInfoResponse;
import com.xmonster.howtaxing.model.MainPopupInfo;
import com.xmonster.howtaxing.repository.main.MainPopupInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MainPopupInfoService {
    private final MainPopupInfoRepository mainPopupInfoRepository;

    // 메인팝업정보 조회
    public Object getMainPopupInfo(){
        log.info(">> [Service]MainPopupInfoService getMainPopupInfo - 메인팝업정보 조회");

        List<MainPopupInfo> mainPopupInfoList = mainPopupInfoRepository.findByIsPost();

        MainPopupInfoResponse mainPopupInfoResponse = null;

        if(mainPopupInfoList != null && mainPopupInfoList.size() == 1){
            MainPopupInfo mainPopupInfo = mainPopupInfoList.get(0);

            if(mainPopupInfo != null){
                mainPopupInfoResponse = MainPopupInfoResponse.builder()
                        .mainPopupId(mainPopupInfo.getMainPopupId())
                        .targetUrl(mainPopupInfo.getTargetUrl())
                        .imageUrl(mainPopupInfo.getImageUrl())
                        .isPost(mainPopupInfo.getIsPost())
                        .build();
            }
        }

        if(mainPopupInfoResponse == null){
            mainPopupInfoResponse = MainPopupInfoResponse.builder()
                    .isPost(false)
                    .build();
        }

        return ApiResponse.success(mainPopupInfoResponse);
    }
}
