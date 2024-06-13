package com.xmonster.howtaxing.feign.jusogov;

import com.xmonster.howtaxing.config.FeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "jusoGovRoadAdrDetail", url="https://business.juso.go.kr", configuration = {FeignConfiguration.class})
public interface JusoGovRoadAdrDetailApi {
    @GetMapping("/addrlink/addrDetailApi.do")
    ResponseEntity<String> getRoadAdrDetailInfo(
            @RequestParam("confmKey") String confmKey,          // 신청시 발급받은 승인키
            @RequestParam("admCd") String admCd,                // 행정구역코드
            @RequestParam("rnMgtSn") String rnMgtSn,            // 도로명코드
            @RequestParam("udrtYn") String udrtYn,              // 지하여부(0:지상, 1:지하)
            @RequestParam("buldMnnm") String buldMnnm,          // 건물본번
            @RequestParam("buldSlno") String buldSlno,          // 건물부번
            @RequestParam("searchType") String searchType,      // 동층호 검색유형(dong, floorho)
            @RequestParam("dongNm") String dongNm,              // 동(층호 검색시 입력)
            @RequestParam("resultType") String resultType       // 검색결과형식 설정(xml, json)
    );
}
