package com.xmonster.howtaxing.controller.house;

import com.xmonster.howtaxing.dto.house.*;
import com.xmonster.howtaxing.dto.jusogov.JusoGovRoadAddrDetailRequest;
import com.xmonster.howtaxing.dto.jusogov.JusoGovRoadAddrListRequest;
import com.xmonster.howtaxing.dto.vworld.PubLandPriceAndAreaRequest;
import com.xmonster.howtaxing.dto.vworld.VworldPubLandPriceAndAreaRequest;
import com.xmonster.howtaxing.model.House;
import com.xmonster.howtaxing.dto.hyphen.HyphenUserSessionRequest;
import com.xmonster.howtaxing.service.house.HouseService;

import com.xmonster.howtaxing.service.house.JusoGovService;
import com.xmonster.howtaxing.service.house.VworldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
@Slf4j
public class HouseController {
    private final HouseService houseService;
    private final JusoGovService jusoGovService;
    private final VworldService vworldService;

    // (취득)주택 도로명주소 조회
    @PostMapping("/house/roadAddr")
    public Object getHouseRoadAddrList(@RequestBody JusoGovRoadAddrListRequest jusoGovRoadAddrListRequest) throws Exception {
        log.info(">> [Controller]HouseController getHouseRoadAddrList - 주택 도로명주소 조회(주소기반산업지원서비스-도로명주소)");
        return jusoGovService.getHouseRoadAddrList(jusoGovRoadAddrListRequest);
    }

    // (취득)주택 도로명주소 상세주소 조회
    @PostMapping("/house/roadAddrDetail")
    public Object getHouseRoadAddrDetail(@RequestBody JusoGovRoadAddrDetailRequest jusoGovRoadAddrDetailRequest) throws Exception {
        log.info(">> [Controller]HouseController getHouseRoadAddrDetail - 주택 도로명주소 상세주소 조회(주소기반산업지원서비스-도로명주소)");
        return jusoGovService.getHouseRoadAddrDetail(jusoGovRoadAddrDetailRequest);
    }

    // (취득)주택 공시가격 및 전용면적 조회 - 브이월드
    @PostMapping("/house/pubLandPriceAndArea")
    public Object getPubLandPriceAndArea(@RequestBody VworldPubLandPriceAndAreaRequest vworldPubLandPriceAndAreaRequest) throws Exception {
        log.info(">> [Controller]HouseController getPubLandPriceAndArea - 주택 공시가격 및 전용면적 조회(브이월드-공동,개별 주택가격속성조회)");
        return vworldService.getPubLandPriceAndArea(vworldPubLandPriceAndAreaRequest);
    }

    // (취득)주택 공시가격 및 전용면적 조회 - DB
    @PostMapping("/house/pubLandPriceAndAreaAtDB")
    public Object getPubLandPriceAndAreaAtDB(@RequestBody PubLandPriceAndAreaRequest PubLandPriceAndAreaRequest) throws Exception {
        log.info(">> [Controller]HouseController getPubLandPriceAndArea - 주택 공시가격 및 전용면적 조회(DB-공동,개별 주택가격속성조회)");
        return vworldService.getPubLandPridAndAreaAtDB(PubLandPriceAndAreaRequest);
    }

    // 보유주택 조회(하이픈-청약홈-주택소유확인)
    @PostMapping("/house/search")
    public Object getHouseListSearch(@RequestBody HouseListSearchRequest houseListSearchRequest) throws Exception {
        log.info(">> [Controller]HouseController getHouseListSearch - 보유주택 조회(하이픈-청약홈-주택소유확인)");
        return houseService.getHouseListSearch(houseListSearchRequest);
    }

    // 보유주택 목록 조회
    @GetMapping("/house/list")
    public Object getHouseList(@RequestParam String calcType) throws Exception {
        log.info(">> [Controller]HouseController getHouseList - 보유주택 목록 조회");
        return houseService.getHouseList(calcType);
    }

    // 보유주택 상세 조회
    @GetMapping("/house/detail")
    public Object getHouseDetail(@RequestParam Long houseId)  throws Exception {
        log.info(">> [Controller]HouseController getHouseDetail - 보유주택 상세 조회");
        return houseService.getHouseDetail(houseId);
    }

    // 보유주택 (직접)등록
    @PostMapping("/house/regist")
    public Object registHouseInfo(@RequestBody HouseRegistRequest houseRegistRequest) throws Exception {
        log.info(">> [Controller]HouseController registHouseInfo - 보유주택 (직접)등록");
        return houseService.registHouseInfo(houseRegistRequest);
    }

    // 보유주택 (정보)수정
    @PutMapping("/house/modify")
    public Object modifyHouseInfo(@RequestBody HouseModifyRequest houseModifyRequest) throws Exception {
        log.info(">> [Controller]HouseController modifyHouseInfo - 보유주택 (정보)수정");
        return houseService.modifyHouseInfo(houseModifyRequest);
    }

    // 보유주택 삭제
    @DeleteMapping("/house/delete")
    public Object deleteHouse(@RequestBody HouseListDeleteRequest houseListDeleteRequest) throws Exception {
        log.info(">> [Controller]HouseController deleteHouse - 보유주택 삭제");
        return houseService.deleteHouse(houseListDeleteRequest);
    }

    // 보유주택 전체 삭제
    @DeleteMapping("/house/deleteAll")
    public Object deleteHouseAll() throws Exception {
        log.info(">> [Controller]HouseController deleteHouseAll - 보유주택 전체 삭제");
        return houseService.deleteHouseAll();
    }

    // (양도)주택 거주기간 조회(하이픈-정부24-주민등록초본)
    @PostMapping("/house/stayPeriod")
    public Object searchHouseStayPeriod(@RequestBody HouseStayPeriodRequest houseStayPeriodRequest) throws Exception {
        return houseService.getHouseStayPeriod(houseStayPeriodRequest);
    }

    // 주소분할 테스트
    @PostMapping("/house/addressParseTest")
    public Object addressSeperateTest(@RequestBody String address) throws Exception {
        return houseService.addressParser(address);
    }

    // 부동산거래내역 기반 매수주택 불러오기(하이픈-청약홈)
    @PostMapping("/house/loadHouse")
    public Object loadHouseFromRealty(@RequestBody HouseListSearchRequest houseListSearchRequest) throws Exception {
        log.info(">> [Controller]HouseController loadHouseFromRealty - 부동산거래내역 기반 매수주택 불러오기");
        return houseService.loadHouseFromRealty(houseListSearchRequest);
    }

    // 세션 기반 주택정보 불려오기
    @PostMapping("/house/getHouseInfo")
    public Object getHouseInfo(@RequestBody HyphenUserSessionRequest hyphenUserSessionRequest) throws Exception {
        log.info(">> [Controller]HouseController getHouseInfo - 세션에 저장된 주택정보 불러오기");
        return houseService.getHouseInfo(hyphenUserSessionRequest);
    }

    // 로드한 정보의 입력값을 모두 채운 후 주택 목록 전체를 보유주택으로 저장
    @PostMapping("/house/saveAllHouse")
    public Object saveAllHouse(@RequestBody HouseSaveAllRequest houseSaveAllRequest) throws Exception {
        log.info(">> [Controller]HouseController saveAllHouse - 보유주택 리스트 일괄 저장");
        return houseService.saveAllHouse(houseSaveAllRequest);
    }

    /*public Object saveAllHouse(@RequestBody List<House> houses) throws Exception {
        //TODO: process POST request
        log.info(">> [Controller]HouseController saveAllHouse - 보유주택 리스트 일괄 저장");

        return houseService.saveAllHouse(houses);
    }*/

    // 재산세 기반 주택 불러오기
    @GetMapping("/house/getEtcHouse")
    public Object getEtcHouse() {
        log.info(">> [Controller]HouseController getEtcHouse - 매수 외 주택 목록");
        return houseService.getEtcHouse();
    }
    
}
