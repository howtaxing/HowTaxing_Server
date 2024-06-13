package com.xmonster.howtaxing.service.house;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.vworld.ApartHousingPriceResponse;
import com.xmonster.howtaxing.dto.vworld.IndvdHousingPriceResponse;
import com.xmonster.howtaxing.dto.vworld.VworldPubLandPriceAndAreaRequest;
import com.xmonster.howtaxing.dto.vworld.VworldPubLandPriceAndAreaResponse;
import com.xmonster.howtaxing.feign.vworld.ApartHousingPriceApi;
import com.xmonster.howtaxing.feign.vworld.IndvdHousingPriceApi;
import com.xmonster.howtaxing.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VworldService {
    private final ApartHousingPriceApi apartHousingPriceApi;
    private final IndvdHousingPriceApi indvdHousingPriceApi;

    private final static int NUM_OF_ROWS = 10;

    @Value("${vworld.key}")
    private String key;

    // (취득)주택 공시가격 및 전용면적 조회
    public Object getPubLandPriceAndArea(VworldPubLandPriceAndAreaRequest vworldPubLandPriceAndAreaRequest){
        log.info(">> [Service]VworldService getPubLandPriceAndArea - (취득)주택 공시가격 및 전용면적 조회");

        // (취득)주택 공시가격 및 전용면적 조회 유효성 체크
        validationCheckForGetPubLandPriceAndArea(vworldPubLandPriceAndAreaRequest);

        String pnu = vworldPubLandPriceAndAreaRequest.getPnu();
        String bdKdcd = StringUtils.defaultString(vworldPubLandPriceAndAreaRequest.getBdKdcd());
        //String stdYear = Integer.toString(LocalDate.now().getYear() - 1); // 기준년도 : 작년
        String dongNm = StringUtils.defaultString(vworldPubLandPriceAndAreaRequest.getDongNm());
        String hoNm = StringUtils.defaultString(vworldPubLandPriceAndAreaRequest.getHoNm());
        Integer numOfRows = (vworldPubLandPriceAndAreaRequest.getNumOfRows() != null) ? vworldPubLandPriceAndAreaRequest.getNumOfRows() : NUM_OF_ROWS;
        Integer pageNo = (vworldPubLandPriceAndAreaRequest.getPageNo() != null) ? vworldPubLandPriceAndAreaRequest.getPageNo() : 1;

        ResponseEntity<?> response = null;

        // 공동주택(아파트, 연립주택, 다세대주택, 빌라 등)
        if(ONE.equals(bdKdcd)){
            response = apartHousingPriceApi.getApartHousingPriceAttr(
                    pnu,
                    EMPTY,
                    dongNm,
                    hoNm,
                    JSON,
                    numOfRows,
                    pageNo,
                    key,
                    EMPTY
            );
        }
        // 비공동주택(단독주택, 다가구주택 등)
        else if(ZERO.equals(bdKdcd)){
            response = indvdHousingPriceApi.getApartHousingPriceAttr(
                    pnu,
                    EMPTY,
                    JSON,
                    numOfRows,
                    pageNo,
                    key,
                    EMPTY
            );
        }

        if(response == null || response.getBody() == null){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_OUTPUT_ERROR, "공공기관(브이월드)에서 응답값을 받지 못했습니다.");
        }

        log.info(response.toString());

        VworldPubLandPriceAndAreaResponse vworldPubLandPriceAndAreaResponse = null;

        String responseData = StringUtils.defaultString(response.getBody().toString());

        // 정상 응답 케이스
        if(responseData.contains("apartHousingPrices") || responseData.contains("indvdHousingPrices")){
            log.info("공공기관 응답 정상");
            if(ONE.equals(bdKdcd)){
                ApartHousingPriceResponse apartHousingPriceResponse = (ApartHousingPriceResponse) convertJsonToHouseData(response.getBody().toString(), ONE);

                if(apartHousingPriceResponse == null
                        || apartHousingPriceResponse.getApartHousingPrices() == null
                        || apartHousingPriceResponse.getApartHousingPrices().getField() == null
                        || apartHousingPriceResponse.getApartHousingPrices().getTotalCount() == null
                        || apartHousingPriceResponse.getApartHousingPrices().getResultCode() == null
                ){
                    throw new CustomException(ErrorCode.HOUSE_JUSOGOV_OUTPUT_ERROR, "공공기관에서 응답값을 받지 못했습니다.");
                }

                String resultCode = StringUtils.defaultString(apartHousingPriceResponse.getApartHousingPrices().getResultCode());
                String resultMsg = StringUtils.defaultString(apartHousingPriceResponse.getApartHousingPrices().getResultMsg());
                int totalCount = Integer.parseInt(apartHousingPriceResponse.getApartHousingPrices().getTotalCount());
                List<ApartHousingPriceResponse.Field> fieldList = apartHousingPriceResponse.getApartHousingPrices().getField();

                if(!EMPTY.equals(resultCode)){
                    log.info("Vworld resultMsg : " + resultMsg);
                    throw new CustomException(ErrorCode.HOUSE_JUSOGOV_OUTPUT_ERROR, "공시가격 및 전용면적 조회 중 오류가 발생했습니다.(공공기관 오류)");
                }

                long pubLandPrice = 0;
                double area = 0;
                int stdrYear = 0;

                if(totalCount > 0){
                    for(ApartHousingPriceResponse.Field field : fieldList){
                        if(stdrYear < Integer.parseInt(field.getStdrYear())){
                            pubLandPrice = field.getPblntfPc();
                            area = field.getPrvuseAr();
                            stdrYear = Integer.parseInt(field.getStdrYear());
                        }
                    }
                }

                vworldPubLandPriceAndAreaResponse = VworldPubLandPriceAndAreaResponse.builder()
                        .hasPubLandPrice(pubLandPrice!=0)
                        .pubLandPrice(pubLandPrice)
                        .hasArea(area!=0)
                        .area(area)
                        .stdrYear(Integer.toString(stdrYear))
                        .build();


            }else{
                // 개별주택은 전용면적과 공시가격을 확인할 수 있는 방법이 없어서 일단 아래와 같이 응답 처리
                //IndvdHousingPriceResponse indvdHousingPriceResponse = (IndvdHousingPriceResponse) convertJsonToHouseData(response.getBody().toString(), TWO);

                vworldPubLandPriceAndAreaResponse = VworldPubLandPriceAndAreaResponse.builder()
                        .hasPubLandPrice(false)
                        .pubLandPrice(null)
                        .hasArea(false)
                        .area(null)
                        .stdrYear(ZERO)
                        .build();
            }
        }
        // 비정상 응답 케이스(데이터가 없는 경우)
        else{
            log.info("공공기관 응답 오류");
            vworldPubLandPriceAndAreaResponse = VworldPubLandPriceAndAreaResponse.builder()
                    .hasPubLandPrice(false)
                    .pubLandPrice(null)
                    .hasArea(false)
                    .area(null)
                    .stdrYear(ZERO)
                    .build();
        }

        return ApiResponse.success(vworldPubLandPriceAndAreaResponse);


        //return convertJsonToHouseData(response.getBody().toString(), bdKdcd);
    }

    // (취득)주택 공시가격 및 전용면적 조회 유효성 체크
    private void validationCheckForGetPubLandPriceAndArea(VworldPubLandPriceAndAreaRequest vworldPubLandPriceAndAreaRequest){
        log.info(">>> VworldService validationCheckForGetPubLandPriceAndArea - (취득)주택 공시가격 및 전용면적 조회 유효성 체크");

        if(vworldPubLandPriceAndAreaRequest == null) throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "공시가격 및 전용면적 조회를 위한 요청 값이 입력되지 않았습니다.");

        String bdKdcd = StringUtils.defaultString(vworldPubLandPriceAndAreaRequest.getBdKdcd());
        String pnu = StringUtils.defaultString(vworldPubLandPriceAndAreaRequest.getPnu());
        
        if(EMPTY.equals(bdKdcd)){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "공시가격 및 전용면적  조회를 위한 공동주택여부 값이 입력되지 않았습니다.");
        }

        if(!ONE.equals(bdKdcd) && !ZERO.equals(bdKdcd)){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "공시가격 및 전용면적  조회를 위한 공동주택여부 값이 올바르지 않습니다.");
        }

        if(EMPTY.equals(pnu)){
            throw new CustomException(ErrorCode.HOUSE_JUSOGOV_INPUT_ERROR, "공시가격 및 전용면적  조회를 위한 고유번호 값이 입력되지 않았습니다.");
        }
    }

    private static Object convertJsonToHouseData(String jsonString, String bdKdcd) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            if(ONE.equals(bdKdcd)){
                return objectMapper.readValue(jsonString, ApartHousingPriceResponse.class);
            }else if(ZERO.equals(bdKdcd)){
                return objectMapper.readValue(jsonString, IndvdHousingPriceResponse.class);
            }else{
                return null;
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CustomException(ErrorCode.HOUSE_HYPHEN_OUTPUT_ERROR);
        }
    }
}