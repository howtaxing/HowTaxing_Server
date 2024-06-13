package com.xmonster.howtaxing.feign.vworld;

import com.xmonster.howtaxing.config.FeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "indvdHousingPrice", url="https://api.vworld.kr", configuration = {FeignConfiguration.class})
public interface IndvdHousingPriceApi {
    @GetMapping("/ned/data/getIndvdHousingPriceAttr")
    ResponseEntity<String> getApartHousingPriceAttr(
            // 참고 : https://www.vworld.kr/dtna/dtna_apiSvcFc_s001.do?apiNum=28
            @RequestParam("pnu") String pnu,                    // 고유번호(8자리 이상)
            @RequestParam("stdYear") String stdYear,            // 기준연도(YYYY: 4자리)
            @RequestParam("format") String format,              // 응답결과 형식(xml 또는 json)
            @RequestParam("numOfRows") Integer numOfRows,       // 검색건수(최대 1000)
            @RequestParam("pageNo") Integer pageNo,             // 페이지 번호
            @RequestParam("key") String key,                    // 발급받은 api key
            @RequestParam("domain") String domain               // API KEY를 발급받을때 입력했던 URL(* HTTPS, FLEX등 웹뷰어가 아닌 브라우저에서의 API사용은 DOMAIN을 추가하여 서비스를 이용할 수 있습니다.)
    );
}