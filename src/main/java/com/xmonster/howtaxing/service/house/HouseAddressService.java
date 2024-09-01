package com.xmonster.howtaxing.service.house;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.house.HouseAddressDto;
import com.xmonster.howtaxing.dto.jusogov.JusoGovRoadAdrResponse.Results.JusoDetail;
import com.xmonster.howtaxing.type.ErrorCode;
import com.xmonster.howtaxing.model.House;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        HouseAddressDto houseAddressDto = new HouseAddressDto(this.replaceLongSpace(address.trim()));
        List<String> splitAddress = splitAddress(houseAddressDto.getAddress());

        try{
            if(!splitAddress.isEmpty()){
                for (int i = 0; i< splitAddress.size(); i++) {
                    String part = splitAddress.get(i);
                    log.debug("part[{}] : {}", i, part);

                    switch (i) {
                        // 시도
                        case 0:
                            if(part.endsWith("시") || part.endsWith("도")){
                                houseAddressDto.setSiDo(part);
                            }
                            break;
                        
                        // 시군구
                        case 1:
                            if(part.endsWith("시") || part.endsWith("군") || part.endsWith("구")){
                                houseAddressDto.setSiGunGu(part);
                            }
                            break;
                        
                        // 구읍면 = 미정
                        // 동리가 = 지번주소 타입
                        // 로길 = 도로명주소 타입
                        case 2:
                            if(part.endsWith("구")){
                                if(houseAddressDto.getSiGunGu() != null && !houseAddressDto.getSiGunGu().isBlank() && !houseAddressDto.getSiGunGu().endsWith("구")){
                                    houseAddressDto.setGu(part);
                                }
                            } else if(part.endsWith("읍") || part.endsWith("면")){
                                houseAddressDto.setEupMyun(part);
                            } else if(part.endsWith("동") || part.endsWith("리") || part.endsWith("가")){
                                houseAddressDto.setDongRi(part);
                                houseAddressDto.setAddressType(1);  // 지번주소로 세팅
                            } else if(part.endsWith("로") || part.endsWith("길")){
                                houseAddressDto.setRoadNm(part);
                                houseAddressDto.setAddressType(2);  // 도로명주소로 세팅
                            }
                            break;
                        
                        // [지번] 지번
                        // [도로명] 건물번호
                        // [미정] 읍면, 동리가, 로길
                        case 3:
                            if(!validAddressType(houseAddressDto, part)) {
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
                            break;

                        // [지번] 동호층
                        // [도로명] 동호층
                        // [기타] 
                        case 4:
                            if(!validAddressType(houseAddressDto, part)) {
                                if(part.endsWith("동")) {
                                    // 동이 없는 케이스는 입력하지않음
                                    if (this.removeFrontZero(part).length() > 1) {
                                        houseAddressDto.setDetailDong(this.removeFrontZero(part));
                                    }
                                } else if(part.endsWith("호")) {
                                    houseAddressDto.setDetailHo(this.removeFrontZero(part));
                                } else if(part.endsWith("층")) {
                                    houseAddressDto.setDetailCheung(this.removeFrontZero(part));
                                } else {
                                    for (String etcAddress : seperateEtcAddress(part, houseAddressDto)) {
                                        houseAddressDto.appendToEtcAddress(etcAddress);
                                    }
                                }
                            }
                            break;

                        // 그 외
                        default:
                            if(part.endsWith("동") && houseAddressDto.getDetailDong() == null){
                                // 동이 없는 케이스는 입력하지않음
                                if (this.removeFrontZero(part).length() > 1) {
                                    houseAddressDto.setDetailDong(this.removeFrontZero(part));
                                }
                            }else if(part.endsWith("호") && houseAddressDto.getDetailHo() == null){
                                houseAddressDto.setDetailHo(this.removeFrontZero(part));
                            }else if(part.endsWith("층") && houseAddressDto.getDetailCheung() == null){
                                houseAddressDto.setDetailCheung(this.removeFrontZero(part));
                            } else{
                                for (String etcAddress : seperateEtcAddress(part, houseAddressDto)) {
                                    houseAddressDto.appendToEtcAddress(etcAddress);
                                }
                            }
                            break;
                    }
                }

                houseAddressDto.makeDetailAddress();    // 상세주소 생성
                houseAddressDto.makeSearchAddress();    // 검색 주소(리스트) 생성

                log.debug("houseAddressDto.toString() : {}", houseAddressDto.toString());
            }
        }catch(Exception e){
            throw  new CustomException(ErrorCode.ADDRESS_SEPARATE_ERROR);
        }

        return houseAddressDto;
    }

    // 정규식 이용한 주소분할 신규로직
    public HouseAddressDto parseAddress(String address) {
        HouseAddressDto houseAddressDto = new HouseAddressDto(address);

        // 도로명주소 여부 판별 정규식
        String roadRegex = "[가-힣A-Za-z·\\d~\\-\\.]+(로|길).[\\d]+";
        Pattern roadPattern = Pattern.compile(roadRegex);
        Matcher roadMatcher = roadPattern.matcher(address);

        // 도로명주소 판별
        if (roadMatcher.find()) {
            // 도로명주소 분할
            String regEx = "([가-힣]+[시|도])\\s([가-힣]+[시|군|구])?\\s?([가-힣]+[구])?\\s?([가-힣]+[읍|면])?\\s?([가-힣A-Za-z·\\d~\\-\\.]+[로|길]).([\\d\\-\\d]+)(.+)?";
            Pattern pattern = Pattern.compile(regEx);
            Matcher matcher = pattern.matcher(address);

            if (matcher.matches()) {
                houseAddressDto.setAddressType(2);

                houseAddressDto.setSiDo(matcher.group(1));
                houseAddressDto.setSiGunGu(matcher.group(2));
                houseAddressDto.setGu(matcher.group(3));
                houseAddressDto.setEupMyun(matcher.group(4));
                houseAddressDto.setRoadNm(matcher.group(5));
                houseAddressDto.setBuildingNo(matcher.group(6));
                if (matcher.group(7) != null) {
                    for (String etcAddress : seperateEtcAddress(matcher.group(7), houseAddressDto)) {
                        houseAddressDto.appendToEtcAddress(etcAddress);
                    }
                }
            } else {
                log.info("주소를 파싱할 수 없습니다. {}", address);
            }
        } else {
            // 지번주소 분할
            String regEx = "([가-힣]+[시|도])\\s([가-힣]+[시|군|구])?\\s?([가-힣]+[구])?\\s?([가-힣]+[읍|면])?\\s?([가-힣]+[동|리])?.([\\d\\-\\d]+)?(.+)?";
            Pattern pattern = Pattern.compile(regEx);
            Matcher matcher = pattern.matcher(address);

            if (matcher.matches()) {
                houseAddressDto.setAddressType(1);

                houseAddressDto.setSiDo(matcher.group(1));
                houseAddressDto.setSiGunGu(matcher.group(2));
                houseAddressDto.setGu(matcher.group(3));
                houseAddressDto.setEupMyun(matcher.group(4));
                houseAddressDto.setDongRi(matcher.group(5));
                houseAddressDto.setJibun(matcher.group(6));
                if (matcher.group(7) != null) {
                    for (String etcAddress : seperateEtcAddress(matcher.group(7), houseAddressDto)) {
                        houseAddressDto.appendToEtcAddress(etcAddress);
                    }
                }
            } else {
                log.info("주소를 파싱할 수 없습니다. {}", address);
            }
        }
        houseAddressDto.makeDetailAddress();    // 상세주소 생성
        houseAddressDto.makeSearchAddress();    // 검색용주소 생성

        return houseAddressDto;
    }

    public JusoDetail replaceSpecialCharactersForJusoDetail(JusoDetail jusoDetail){
        if(jusoDetail != null){
            jusoDetail.setRoadAddr(this.replaceSpecialCharacters(jusoDetail.getRoadAddr()));
            jusoDetail.setRoadAddrPart1(this.replaceSpecialCharacters(jusoDetail.getRoadAddrPart1()));
            jusoDetail.setRoadAddrPart2(this.replaceSpecialCharacters(jusoDetail.getRoadAddrPart2()));
            jusoDetail.setJibunAddr(this.replaceSpecialCharacters(jusoDetail.getJibunAddr()));
            jusoDetail.setDetBdNmList(this.replaceSpecialCharacters(jusoDetail.getDetBdNmList()));
            if (jusoDetail.getBdNm() == null || jusoDetail.getBdNm() == "") {
                jusoDetail.setBdNm("기본보유주택명");
            } else {
                jusoDetail.setBdNm(this.replaceSpecialCharacters(jusoDetail.getBdNm()));
            }
        }

        return jusoDetail;
    }

    // 주소 비교
    public Boolean compareAddress(HouseAddressDto houseAddressDto1, HouseAddressDto houseAddressDto2){
        List<String> searchAddr1 = houseAddressDto1.getSearchAddress();
        List<String> searchAddr2 = houseAddressDto2.getSearchAddress();
        String detailAddr1 = StringUtils.defaultString(houseAddressDto1.getDetailAddress());
        String detailAddr2 = StringUtils.defaultString(houseAddressDto2.getDetailAddress());

        log.debug("첫번째주소 : {} {}", searchAddr1.get(0), detailAddr1);
        log.debug("두번째주소 : {} {}", searchAddr2.get(0), detailAddr2);

        boolean isSame = false;

        // 지번주소 혹은 도로명주소 비교
        if (searchAddr1.get(0).equals(searchAddr2.get(0))) {
            // 상세주소 비교
            if (detailAddr1.equals(detailAddr2)) {
                isSame = true;
            }
        }

        return isSame;
    }
    
    // 주소 비교 오버로딩
    public Boolean compareAddress(HouseAddressDto houseAddressDto1, House house) {
        List<String> searchAddr1 = houseAddressDto1.getSearchAddress();
        String searchAddr2 = EMPTY;
        if (houseAddressDto1.getAddressType() == 1) {
            searchAddr2 = house.getJibunAddr();
        } else {
            searchAddr2 = house.getRoadAddr();
        }

        if (searchAddr1 == null || searchAddr1.isEmpty()) {
            log.warn("주소 리스트가 비어 있습니다. 비교를 할 수 없습니다.");
            return false;
        }

        String compareAddr1 = searchAddr1.get(0) + (searchAddr1.size() > 1 ? " " + searchAddr1.get(1) : "");

        log.info("첫번째주소 : {}", compareAddr1);
        log.info("두번째주소 : {}", searchAddr2);

        boolean isSame = searchAddr2.contains(compareAddr1);

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

        /*
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
        */

        // 도로명주소도 하이픈이 포함된 경우가 있어 주소타입 분기 제외
        regex = "^[0-9-]+$";
        result = input.matches(regex);
        
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

    // 정규식으로 주소분리(공백, 쉼표, 괄호묶음)
    private static List<String> splitAddress(String address) {
        List<String> result = new ArrayList<>();
        // Regular expression to match everything except the contents inside the parentheses
        Pattern pattern = Pattern.compile("([^(),\\s]+|\\([^()]*\\))");
        Matcher matcher = pattern.matcher(address);

        while (matcher.find()) {
            result.add(matcher.group().trim());
        }

        return result;
    }

    // 주소타입 확정여부 검증
    private boolean validAddressType(HouseAddressDto houseAddressDto, String part) {
        // [지번주소] 지번 입력 안된 경우 입력
        if(houseAddressDto.getAddressType() == 1 && houseAddressDto.getJibun() == null) {
            if(this.isValidFormat(1, part)){
                houseAddressDto.setJibun(part);
            }
            return true;
        // [도로명주소] 건물번호 입력 안된 경우 입력
        } else if(houseAddressDto.getAddressType() == 2 && houseAddressDto.getBuildingNo() == null) {
            if(this.isValidFormat(2, part)){
                houseAddressDto.setBuildingNo(part);
            }
            return true;
        } else {
            return false;
        }
    }

    // 기타주소 입력 처리
    private List<String> seperateEtcAddress(String part, HouseAddressDto houseAddressDto) {
        part = part.replaceAll("[()]", "").trim();  // 괄호 제거 및 트림
        // String[] parts = part.split("\\s*,\\s*|\\s+");  // 공백과 쉼표로 분리
        String[] parts = part.split("\\s*,\\s*|\\s+|\\s*(?<=[가-힣])-");  // 공백과 쉼표, 대시로 분리
        List<String> etcParts = new ArrayList<>();

        for (String p : parts) {
            // 지번주소의 동명이 들어있는 경우 분리 후 입력 - 숫자형태 동은 동/리가 아님
            if (p.endsWith("동") && houseAddressDto.getDongRi() == null && !p.matches("^[\\d]{1,5}[동]")) {
                houseAddressDto.setDongRi(p);
            // 숫자형태 동 입력
            } else if (removeFrontZero(p).matches("^[\\d]{1,5}[동]")) {
                houseAddressDto.setDetailDong(removeFrontZero(p));
            // 숫자형태 호 입력
            } else if (p.matches("^[\\d]{1,5}[호]")) {
                houseAddressDto.setDetailHo(removeFrontZero(p));
            // 별표 포함된 동호수는 버림
            } else if (p.matches("([\\*]+[동])?(\\-)?([\\*]+[호])?")) {
                continue;
            } else if (p.matches("(동|외|[\\d+]+필지|산[\\d]+)")) {
                // 외, 산, 필지 등 제외
                continue;
            } else if (p.contains("BL")) {
                // 재개발구역 제외
                continue;
            } else {
                // 지번주소의 지번이나 도로명주소의 건물번호가 입력이 되어있는 경우, 숫자와 하이픈 타입이면 하이픈으로 분리하여 동호수로 입력
                if ((houseAddressDto.getAddressType() == 1 && houseAddressDto.getJibun() != null) || (houseAddressDto.getAddressType() == 2 && houseAddressDto.getBuildingNo() != null))
                {
                    if (p.matches("^([\\d]+|[\\d]+[동])\\-([\\d]+|[\\d]+[호])$") && p.length() > 1) {
                        String[] dongPart = p.split("-");
                        log.info("동호수로 분리 : {}동 {}호", dongPart[0], dongPart[1]);

                        if (houseAddressDto.getDetailDong() == null) {
                            houseAddressDto.setDetailDong(dongPart[0] + (dongPart[0].endsWith("동") ? "" : "동"));
                        }
                        if (houseAddressDto.getDetailHo() == null) {
                            houseAddressDto.setDetailHo(dongPart[1] + (dongPart[1].endsWith("호") ? "" : "호"));
                        }
                        continue;
                    }
                }
                etcParts.add(p);
            }
        }

        return etcParts;
    }
}
