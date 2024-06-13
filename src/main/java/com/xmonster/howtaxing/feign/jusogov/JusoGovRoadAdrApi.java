package com.xmonster.howtaxing.feign.jusogov;

import com.xmonster.howtaxing.config.FeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "jusoGovRoadAdr", url="https://business.juso.go.kr", configuration = {FeignConfiguration.class})
public interface JusoGovRoadAdrApi {
    @GetMapping("/addrlink/addrLinkApi.do")
    ResponseEntity<String> getRoadAdrInfo(
            @RequestParam("confmKey") String confmKey,              // 신청 시 발급받은 승인키
            @RequestParam("currentPage") Integer currentPage,       // 현재 페이지 번호(n>0)
            @RequestParam("countPerPage") Integer countPerPage,     // 페이지당 출력할 결과 row 수(0<n<=100)
            @RequestParam("keyword") String keyword,                // 주소 검색어
            @RequestParam("resultType") String resultType,          // 검색결과 형식 설정(기본 XML 형식), json 입력 시 JSON 형식의 결과 제공
            @RequestParam("hstryYn") String hstryYn,                // (* 2020년 12월 8일 추가된 항목) 변동된 주소정보 포함 여부
            @RequestParam("firstSort") String firstSort,            // (* 2020년 12월 8일 추가된 항목) 정확도 정렬(none), 우선정렬(road:도로명 포함, location:지번 포함) ※ keyword(검색어)가 우선정렬 항목에 포함된 결과 우선 표출
            @RequestParam("addInfoYn") String addInfoYn             // (* 2020년 12월 8일 추가된 항목) 출력결과에 추가된 항목(hstryYn, relJibun, hemdNm) 제공여부 ※ 해당 옵션으로 추가 제공되는 항목의 경우, 추후 특정항목이 제거되거나 추가될 수 있으니 적용 시 고려해주시기 바랍니다.
    );
}