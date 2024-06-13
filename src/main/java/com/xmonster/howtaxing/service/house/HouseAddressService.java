package com.xmonster.howtaxing.service.house;

import com.xmonster.howtaxing.dto.house.HouseAddressDto;
import com.xmonster.howtaxing.dto.jusogov.JusoGovRoadAdrResponse.Results.JusoDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HouseAddressService {

    /*****************************************************************************
     * list3 예시)
     * 강원도 원주시 명륜동 856 더샵 원주센트럴파크 2단지 0207동 00401호
     * 경기도 시흥시 광석동 542 시흥시청역동원로얄듀크 0102동 01403호
     *
     * list1 예시)
     * 경기도 시흥시 시흥대로 404(광석동)
     * 강원특별자치도 원주시 서원대로 290(명륜동)
     *
     * list2 예시)
     * 강원특별자치도 원주시 명륜동 산31 원주 더샵 센트럴파크 2단지 ****동-****호
     * 경기도 안양만안구 안양동 576-1 안양 명학역 유보라 더 스마트 ****동-****호
     * 경기도 의왕시 포일동 643 위브호수마을2단지 ****동-****호
     * 경기도 시흥시 광석동 BL-B-7 시흥시청역 동원로얄듀크 ****동-****호
     * 경기도 부천시 상동 394 한아름마을(한국) ****동-****호
     * 서울특별시 관악구 봉천동 1717 관악푸르지오 ****동-****호
     * 경기도 안양만안구 안양동 576-1 안양 명학역 유보라 더 스마트 ****동-****호
     * 경기도 의왕시 포일동 643 위브호수마을2단지 ****동-****호
     * 서울특별시 관악구 봉천동 1717 관악푸르지오 ****동-****호
     * 경기도 부천시 상동 394 한국아파트 ****동-****호
     *****************************************************************************/

    // 주소 분할
    public HouseAddressDto separateAddress(String address){
        log.info(">>> HouseAddressService separateAddress - 주소 분할");

        HouseAddressDto houseAddressDto = new HouseAddressDto(this.replaceLongSpace(address));
        String[] splitAddress = houseAddressDto.getAddress().split("\\s+|\\(");    // 공백과 '('로 분할

        if(splitAddress.length > 0){
            log.info("----- splitAddress Print Start -----");
            for(int i=0; i<splitAddress.length; i++){
                log.info("Index : " + i);
                log.info("Data : " + splitAddress[i]);
            }
            log.info("----- splitAddress Print End -----");

            for(int j=0; j<splitAddress.length; j++){
                String str = splitAddress[j];

                if(str.endsWith(")") && !str.startsWith("(")){
                    splitAddress[j] = "(" + str;
                }
            }

            log.info("----- Mapping Start -----");
            for(int i=0; i<splitAddress.length; i++){
                String part = splitAddress[i];
                log.info("index : " + i);
                log.info("part : " + part);

                // [1] 시/도
                if(i==0){
                    if(part.endsWith("시") || part.endsWith("도")){
                        houseAddressDto.setSiDo(part);
                    }
                }
                // [2] 시/군/구
                else if(i==1){
                    if(part.endsWith("시") || part.endsWith("군") || part.endsWith("구")){
                        houseAddressDto.setSiGunGu(part);
                    }
                }
                // [3] 구 or 읍/면 or 동/리/가 or 도로명
                else if(i==2){
                    if(part.endsWith("구")){
                        if(houseAddressDto.getSiGunGu() != null && !houseAddressDto.getSiGunGu().isBlank() && !houseAddressDto.getSiGunGu().endsWith("구")){
                            houseAddressDto.setGu(part);
                        }
                    } else if(part.endsWith("읍") || part.endsWith("면")){
                        houseAddressDto.setEupMyun(part);
                    }else if(part.endsWith("동") || part.endsWith("리") || part.endsWith("가")){
                        houseAddressDto.setDongRi(part);
                        houseAddressDto.setAddressType(1);  // 지번주소로 세팅
                    }else if(part.endsWith("로") || part.endsWith("길")){
                        houseAddressDto.setRoadNm(part);
                        houseAddressDto.setAddressType(2);  // 도로명주소로 세팅
                    }
                }
                // [4] 3가지 케이스
                // [지번주소] 지번
                // [도로명주소] 건물번호
                // [모름] 읍/면 or 동/리/가 or 로/길
                else if(i==3){
                    if(houseAddressDto.getAddressType() == 1){
                        // 지번을 아직 세팅하지 않은 경우
                        if(EMPTY.equals(StringUtils.defaultString(houseAddressDto.getJibun()))){
                            // 지번(숫자와 하이픈만으로 이루어진 문자열인지 체크)
                            if(this.isValidFormat(1, part)){
                                houseAddressDto.setJibun(part);
                            }
                        }
                    }else if(houseAddressDto.getAddressType() == 2){
                        // 건물번호(숫자만으로 이루어진 문자열인지 체크)
                        if(this.isValidFormat(2, part)){
                            houseAddressDto.setBuildingNo(part);
                        }
                    }else{
                        if(part.endsWith("읍") || part.endsWith("면")){
                            houseAddressDto.setEupMyun(part);
                        }else if(part.endsWith("동") || part.endsWith("리") || part.endsWith("가")){
                            houseAddressDto.setDongRi(part);
                            houseAddressDto.setAddressType(1);  // 지번주소로 세팅
                        }else if(part.endsWith("로") || part.endsWith("길")){
                            houseAddressDto.setRoadNm(part);
                            houseAddressDto.setAddressType(2);  // 도로명주소로 세팅
                        }
                    }
                }
                // [5] 3가지 케이스
                // [지번주소] 지번 or 동/호/층 or ELSE
                // [도로명주소] 건물번호 or 동/호/층 / or ELSE
                // [모름] 동/리/가 or 로/길
                else if(i==4){
                    if(houseAddressDto.getAddressType() == 1){
                        // 지번을 아직 세팅하지 않은 경우
                        if(EMPTY.equals(StringUtils.defaultString(houseAddressDto.getJibun()))){
                            // 지번(숫자와 하이픈만으로 이루어진 문자열인지 체크)
                            if(this.isValidFormat(1, part)){
                                houseAddressDto.setJibun(part);
                            }else{
                                houseAddressDto.appendToEtcAddress(part);
                            }
                        }
                        // 지번을 이미 세팅한 경우
                        else{
                            if(part.endsWith("동")){
                                houseAddressDto.setDetailDong(this.removeFrontZero(part));
                            }else if(part.endsWith("호")){
                                houseAddressDto.setDetailHo(this.removeFrontZero(part));
                            }else if(part.endsWith("층")){
                                houseAddressDto.setDetailCheung(this.removeFrontZero(part));
                            }else if(this.isValidFormat(1, part)){
                                String[] detailAddr = part.split(HYPHEN);
                                if(detailAddr != null && detailAddr.length == 2){
                                    houseAddressDto.setDetailDong(this.removeFrontZero(detailAddr[0])); // 동
                                    houseAddressDto.setDetailHo(this.removeFrontZero(detailAddr[1]));   // 호
                                }
                            }else{
                                houseAddressDto.appendToEtcAddress(part);
                            }
                        }
                    }else if(houseAddressDto.getAddressType() == 2){
                        // 건물번호(숫자만으로 이루어진 문자열인지 체크)
                        if(this.isValidFormat(2, part)){
                            houseAddressDto.setBuildingNo(part);
                        }else if(part.endsWith("동")){
                            houseAddressDto.setDetailDong(this.removeFrontZero(part));
                        }else if(part.endsWith("호")){
                            houseAddressDto.setDetailHo(this.removeFrontZero(part));
                        }else if(part.endsWith("층")){
                            houseAddressDto.setDetailCheung(this.removeFrontZero(part));
                        }else{
                            houseAddressDto.appendToEtcAddress(part);
                        }
                    }else{
                        if(part.endsWith("동") || part.endsWith("리") || part.endsWith("가")){
                            houseAddressDto.setDongRi(part);
                            houseAddressDto.setAddressType(1);  // 지번주소로 세팅
                        }else if(part.endsWith("로") || part.endsWith("길")){
                            houseAddressDto.setRoadNm(part);
                            houseAddressDto.setAddressType(2);  // 도로명주소로 세팅
                        }else{
                            houseAddressDto.appendToEtcAddress(part);
                        }
                    }
                }
                // [6] 2가지 케이스 (반복 수행)
                // [지번주소] 지번 or 동/호/층 or ELSE
                // [도로명주소] 건물번호 or 동/호/층 / or ELSE
                else{
                    if(houseAddressDto.getAddressType() == 1){
                        // 지번을 아직 세팅하지 않은 경우
                        if(EMPTY.equals(StringUtils.defaultString(houseAddressDto.getJibun()))){
                            // 지번(숫자와 하이픈만으로 이루어진 문자열인지 체크)
                            if(this.isValidFormat(1, part)){
                                if(houseAddressDto.getJibun() == null){
                                    houseAddressDto.setJibun(part);
                                }
                            }else{
                                houseAddressDto.appendToEtcAddress(part);
                            }
                        }
                        // 지번을 이미 세팅한 경우
                        else{
                            if(part.endsWith("동")){
                                if(houseAddressDto.getDetailDong() == null){
                                    houseAddressDto.setDetailDong(this.removeFrontZero(part));
                                }
                            }else if(part.endsWith("호")){
                                if(houseAddressDto.getDetailHo() == null){
                                    houseAddressDto.setDetailHo(this.removeFrontZero(part));
                                }
                            }else if(part.endsWith("층")){
                                if(houseAddressDto.getDetailCheung() == null){
                                    houseAddressDto.setDetailCheung(this.removeFrontZero(part));
                                }
                            }else if(this.isValidFormat(1, part)){
                                String[] detailAddr = part.split(HYPHEN);
                                if(detailAddr != null && detailAddr.length == 2){
                                    houseAddressDto.setDetailDong(this.removeFrontZero(detailAddr[0])); // 동
                                    houseAddressDto.setDetailHo(this.removeFrontZero(detailAddr[1]));   // 호
                                }
                            }else{
                                houseAddressDto.appendToEtcAddress(part);
                            }
                        }
                    }else if(houseAddressDto.getAddressType() == 2){
                        // 건물번호(숫자만으로 이루어진 문자열인지 체크)
                        if(this.isValidFormat(2, part)){
                            if(houseAddressDto.getBuildingNo() == null){
                                houseAddressDto.setBuildingNo(part);
                            }
                        }else if(part.endsWith("동")){
                            if(houseAddressDto.getDetailDong() != null){
                                houseAddressDto.setDetailDong(this.removeFrontZero(part));
                            }
                        }else if(part.endsWith("호")){
                            if(houseAddressDto.getDetailHo() != null){
                                houseAddressDto.setDetailHo(this.removeFrontZero(part));
                            }
                        }else if(part.endsWith("층")){
                            if(houseAddressDto.getDetailCheung() != null){
                                houseAddressDto.setDetailCheung(this.removeFrontZero(part));
                            }
                        }else{
                            houseAddressDto.appendToEtcAddress(part);
                        }
                    }else{
                        houseAddressDto.appendToEtcAddress(part);
                    }
                }
            }
            log.info("----- Mapping End -----");

            houseAddressDto.makeDetailAddress();    // 상세주소 생성
            houseAddressDto.makeSearchAddress();    // 검색 주소(리스트) 생성

            log.info("houseAddressDto.toString() : " + houseAddressDto.toString());
        }

        return houseAddressDto;
    }

    public JusoDetail replaceSpecialCharactersForJusoDetail(JusoDetail jusoDetail){
        if(jusoDetail != null){
            jusoDetail.setRoadAddr(this.replaceSpecialCharacters(jusoDetail.getRoadAddr()));
            jusoDetail.setRoadAddrPart1(this.replaceSpecialCharacters(jusoDetail.getRoadAddrPart1()));
            jusoDetail.setRoadAddrPart2(this.replaceSpecialCharacters(jusoDetail.getRoadAddrPart2()));
            jusoDetail.setJibunAddr(this.replaceSpecialCharacters(jusoDetail.getJibunAddr()));
            jusoDetail.setDetBdNmList(this.replaceSpecialCharacters(jusoDetail.getDetBdNmList()));
            jusoDetail.setBdNm(this.replaceSpecialCharacters(jusoDetail.getBdNm()));
        }

        return jusoDetail;
    }

    // 주소 비교
    public Boolean compareAddress(HouseAddressDto houseAddressDto1, HouseAddressDto houseAddressDto2){
        List<String> searchAddr1 = houseAddressDto1.getSearchAddress();
        List<String> searchAddr2 = houseAddressDto2.getSearchAddress();
        String detailAddr1 = StringUtils.defaultString(houseAddressDto1.getDetailAddress());
        String detailAddr2 = StringUtils.defaultString(houseAddressDto2.getDetailAddress());

        boolean isSame = true;

        if(searchAddr1 != null && !searchAddr1.isEmpty() && searchAddr2 != null && !searchAddr2.isEmpty()){
            if(searchAddr1.size() != searchAddr2.size()){
                isSame = false;
            }else{
                for(int i=0; i<searchAddr1.size(); i++){
                    if(!searchAddr1.get(i).equals(searchAddr2.get(i))){
                        isSame = false;
                    }
                }
            }
        }else{
            isSame = false;
        }

        if(isSame){
            if(EMPTY.equals(detailAddr1) || EMPTY.equals(detailAddr2)){
                isSame = false;
            }else{
                if(!detailAddr1.equals(detailAddr2)){
                    isSame = false;
                }
            }
        }

        return isSame;
    }

    private String replaceSpecialCharacters(String address){
        String replaceAddress = address;

        if(address != null){
            replaceAddress = replaceAddress.replaceAll("&nbsp;", SPACE);
            replaceAddress = replaceAddress.replaceAll("&amp;", "&");
            replaceAddress = replaceAddress.replaceAll("&lt;", "<");
            replaceAddress = replaceAddress.replaceAll("&gt;", ">");
        }

        return replaceAddress;
    }

    private String replaceLongSpace(String address){
        String replaceAddress = StringUtils.defaultString(address);

        replaceAddress = replaceAddress.replaceAll("\\s+", SPACE);

        return replaceAddress;
    }

    private boolean isValidFormat(int type, String input){
        boolean result = false;
        String regex = EMPTY;

        // 숫자와 하이픈만으로 이루어진 문자열인지 체크
        if(type == 1){
            regex = "^[0-9-]+$";
            result = input.matches(regex);
        }
        // 숫자만으로 이루어진 문자열인지 체크
        else if(type == 2){
            regex = "^[0-9]+$";
            result = input.matches(regex);
        }

        return result;
    }

    private String removeFrontZero(String input){
        String result = EMPTY;
        String regex = "^0+";

        if(input != null && !input.isBlank()){
            result = input.replaceFirst(regex, EMPTY);
        }

        return result;
    }
}
