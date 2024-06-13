package com.xmonster.howtaxing.dto.house;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ToString
public class HouseAddressDto {
    private String address;                 // 주소(원장주소)
    private String detailAddress;           // 상세주소
    private List<String> searchAddress;     // 검색주소

    private int addressType;                // 주소유형(1:지번주소, 2:도로명주소)
    private String siDo;                    // (공통)시도
    private String siGunGu;                 // (공통)시군구
    private String gu;
    private String eupMyun;                 // (공통)읍면
    private String dongRi;                  // (지번)동리
    private String jibun;                   // (지번)지번
    private String roadNm;                  // (도로명)도로명
    private String buildingNo;              // (도로명)건물번호
    private String coHouseNm;               // (지번)공동주택명

    private String detailDong;              // (상세주소)동
    private String detailHo;                // (상세주소)호
    private String detailCheung;            // (상세주소)층

    private List<String> etcAddress;        // 기타주소

    public HouseAddressDto(String address){
        this.address = address;
        this.detailAddress = EMPTY;
        this.addressType = 0;
        this.searchAddress = new ArrayList<>();
        this.etcAddress = new ArrayList<>();
    }

    public void appendToEtcAddress(String addressComponent){
        this.etcAddress.add(addressComponent);
    }

    // 상세주소 생성
    public void makeDetailAddress(){
        String dtAddr = EMPTY;

        dtAddr = this.appendStringWithSpace(this.detailDong, dtAddr);
        dtAddr = this.appendStringWithSpace(this.detailHo, dtAddr);
        dtAddr = this.appendStringWithSpace(this.detailCheung, dtAddr);

        this.detailAddress = dtAddr;
    }

    // 검색 주소(리스트) 생성
    public void makeSearchAddress(){
        String scAddr = EMPTY;

        /* Common Part */
        scAddr = this.appendStringWithSpace(this.siDo, scAddr);              // 시/도
        scAddr = this.appendStringWithSpace(this.siGunGu, scAddr);           // 시/군/구
        scAddr = this.appendStringWithSpace(this.gu, scAddr);                // 구
        scAddr = this.appendStringWithSpace(this.eupMyun, scAddr);           // 읍/면

        /* Jibun Addr Part */
        if(this.addressType == 1){
            scAddr = this.appendStringWithSpace(this.dongRi, scAddr);        // 동/리/가
            scAddr = this.appendStringWithSpace(this.jibun, scAddr);         // 지번
        }
        /* Road Addr Part */
        else if(this.addressType == 2){
            scAddr = this.appendStringWithSpace(this.roadNm, scAddr);        // 로/길
            scAddr = this.appendStringWithSpace(this.buildingNo, scAddr);    // 건물번호
        }
        // ETC
        else{
            scAddr = this.address;
        }

        // 지번주소 : 시/도 + 시/군/구 + (구) + (읍/면) + 동/리 + 지번
        // 도로명주소 : 시/도 + 시/군/구 + (구) + (읍/면) + 로/길 + 건물번호
        this.searchAddress.add(scAddr);
        
        if(this.etcAddress != null && !this.etcAddress.isEmpty()){
            this.searchAddress.addAll(this.etcAddress);
        }
    }

    // 공백과 함께 문자열 추가
    private String appendStringWithSpace(String part, String total){
        StringBuilder result = new StringBuilder((total == null) ? EMPTY : total);

        if(part != null && !EMPTY.equals(part)){
            if(!EMPTY.contentEquals(result)){
                result.append(SPACE);
            }
            result.append(part);
        }

        return result.toString();
    }
}
