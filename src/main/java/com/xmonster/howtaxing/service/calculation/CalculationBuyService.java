package com.xmonster.howtaxing.service.calculation;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultRequest;
import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultResponse;
import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultResponse.CalculationBuyOneResult;
import com.xmonster.howtaxing.dto.common.ApiResponse;

import com.xmonster.howtaxing.model.*;
import com.xmonster.howtaxing.repository.calculation.*;
import com.xmonster.howtaxing.repository.house.HouseRepository;
import com.xmonster.howtaxing.service.house.HouseAddressService;
import com.xmonster.howtaxing.type.ErrorCode;
import com.xmonster.howtaxing.utils.HouseUtil;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CalculationBuyService {
    private final HouseAddressService houseAddressService;

    private final CalculationBuyResultRepository calculationBuyResultRepository;
    private final CalculationProcessRepository calculationProcessRepository;
    private final TaxRateInfoRepository taxRateInfoRepository;
    private final DeductionInfoRepository deductionInfoRepository;
    private final HouseRepository houseRepository;

    private final UserUtil userUtil;
    private final HouseUtil houseUtil;

    private Class<?> calculationBranchClass;
    private CalculationBranch target;

    // 취득세 계산 결과 조회
    public Object getCalculationBuyResult(CalculationBuyResultRequest calculationBuyResultRequest){
        log.info(">> [Service]CalculationBuyService getCalculationBuyResult - 취득세 계산 결과 조회");

        // 요청 데이터 유효성 검증
        validationCheckRequestData(calculationBuyResultRequest);

        // 분기 메소드
        try{
            calculationBranchClass = Class.forName("com.xmonster.howtaxing.service.calculation.CalculationBuyService$CalculationBranch");
            target = new CalculationBranch();

            Method method = calculationBranchClass.getMethod("calculationStart", CalculationBuyResultRequest.class);

            Object result = method.invoke(target, calculationBuyResultRequest);

            return ApiResponse.success(result);

        }catch(Exception e){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
        }
    }

    // 요청 데이터 유효성 검증
    private void validationCheckRequestData(CalculationBuyResultRequest calculationBuyResultRequest){
        log.info(">>> CalculationBranch validationCheck - 요청 데이터 유효성 검증");

        if(calculationBuyResultRequest == null) throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택 정보가 입력되지 않았습니다.");

        String houseType = StringUtils.defaultString(calculationBuyResultRequest.getHouseType());
        String houseName = StringUtils.defaultString(calculationBuyResultRequest.getHouseName());
        String detailAdr = StringUtils.defaultString(calculationBuyResultRequest.getDetailAdr());
        LocalDate buyDate = calculationBuyResultRequest.getBuyDate();
        Long buyPrice = calculationBuyResultRequest.getBuyPrice();
        Boolean isPubLandPriceOver100Mil = calculationBuyResultRequest.getIsPubLandPriceOver100Mil();
        String roadAddr = StringUtils.defaultString(calculationBuyResultRequest.getRoadAddr());
        Boolean isAreaOver85 = calculationBuyResultRequest.getIsAreaOver85();
        Boolean isDestruction = calculationBuyResultRequest.getIsDestruction();
        Integer ownerCnt = calculationBuyResultRequest.getOwnerCnt();
        Integer userProportion = calculationBuyResultRequest.getUserProportion();
        Boolean isMoveInRight = calculationBuyResultRequest.getIsMoveInRight();
        Boolean hasSellPlan = calculationBuyResultRequest.getHasSellPlan();
        Boolean isOwnHouseCntRegist = calculationBuyResultRequest.getIsOwnHouseCntRegist();

        if(EMPTY.equals(houseType)){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 주택유형 정보가 입력되지 않았습니다.");
        }

        if(!ONE.equals(houseType) && !TWO.equals(houseType) && !THREE.equals(houseType) && !FOUR.equals(houseType) && !FIVE.equals(houseType) && !SIX.equals(houseType)){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 주택유형 정보가 올바르지 않습니다.");
        }

        if(EMPTY.equals(houseName)){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 주택명 정보가 입력되지 않았습니다.");
        }

        if(EMPTY.equals(detailAdr)){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 상세주소 정보가 입력되지 않았습니다.");
        }

        if(buyDate == null){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 취득일자 정보가 입력되지 않았습니다.");
        }

        if(buyPrice == null || buyPrice <= 0){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 취득금액 정보가 올바르지 않습니다.");
        }

        if(isPubLandPriceOver100Mil == null){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 공시지가1억초과여부 정보가 입력되지 않았습니다.");
        }

        if(EMPTY.equals(roadAddr)){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 도로명주소 정보가 입력되지 않았습니다.");
        }

        if(isAreaOver85 == null){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 전용면적85제곱미터초과여부 정보가 입력되지 않았습니다.");
        }

        if(isDestruction == null){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 멸실여부 정보가 입력되지 않았습니다.");
        }

        if(ownerCnt == null || ownerCnt <= 0){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 소유자수 정보가 올바르지 않습니다.");
        }

        if(ownerCnt > 2){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "소유자 수는 1명 또는 2명이어야 합니다.");
        }

        if(ownerCnt >= 2 && userProportion == 100){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "소유자가 2인 이상인 경우 본인지분비율을 100% 미만이어야 합니다.");
        }

        if(userProportion == null || userProportion <= 0){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 본인지분비율 정보가 올바르지 않습니다.");
        }

        if(isMoveInRight == null){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 입주권여부 정보가 입력되지 않았습니다.");
        }

        if(hasSellPlan == null){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 양도예정여부 정보가 입력되지 않았습니다.");
        }

        if(isOwnHouseCntRegist == null){
            throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득주택의 보유주택수직접입력여부 정보가 입력되지 않았습니다.");
        }
    }

    private class CalculationBranch {
        // Start
        public Object calculationStart(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBuyService calculationStart - 계산 시작");
            CalculationBuyResultResponse calculationBuyResultResponse;

            try{
                Method method = calculationBranchClass.getMethod("branchNo001", CalculationBuyResultRequest.class);
                calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
            }catch(Exception e){
                throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 001
         * 분기명 : 취득하려는 주택의 유형
         * 분기설명 : 취득 유형 사용자 선택(주택, 준공분양권, 입주권 중 택1)
         */
        public CalculationBuyResultResponse branchNo001(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo001 - 취득세 분기번호 001 : 취득하려는 주택의 유형");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;
            
            // 주택유형(1:아파트 2:연립,다가구 3:입주권 4:단독주택,다세대 5:분양권(주택) 6:주택)
            String houseType = calculationBuyResultRequest.getHouseType();
            
            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "001")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(FIVE.equals(houseType)){
                selectNo = 2;   // 준공 분양권(선택번호:2)
            } else if(THREE.equals(houseType)){
                selectNo = 3;   // 입주권(선택번호:3)
            } else{
                selectNo = 1;   // 주택(선택번호:1)
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 002
         * 분기명 : 분양권 취득일자
         * 분기설명 : 2020.07.10 이전 취득, 2020.07.11~2020.08.11 취득, 2020.08.12 이후 취득으로 분류
         */
        public CalculationBuyResultResponse branchNo002(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo002 - 취득세 분기번호 002 : 분양권 취득일자");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            String buyDate = EMPTY;
            if(calculationBuyResultRequest != null && calculationBuyResultRequest.getBuyDate() != null){
                buyDate = calculationBuyResultRequest.getBuyDate().toString().replace(HYPHEN, EMPTY);
            }

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "002")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_DATE, buyDate, variableData, dataMethod)){
                    selectNo = calculationProcess.getCalculationProcessId().getSelectNo();
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());

                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 003
         * 분기명 : 분양권 준공시점(잔금지급일) 총 주택 수
         * 분기설명 : 잔금지급일(준공시점)을 사용자에게 입력 받아 해당 시점 신규 분양권 포함 총 주택 수를 계산
         */
        public CalculationBuyResultResponse branchNo003(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo003 - 취득세 분기번호 003 : 분양권 준공시점(잔금지급일) 총 주택 수");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "003")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // (취득주택 포함)보유주택 수
            long ownHouseCount = getOwnHouseCount(calculationBuyResultRequest);

            // 1~3주택
            if(ownHouseCount >= 1 && ownHouseCount <= 3){
                selectNo = 1;
            }
            // 4주택 이상
            else if(ownHouseCount >= 4){
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 004
         * 분기명 : 분양권 준공시점(잔금지급일) 총 주택 수
         * 분기설명 : 잔금지급일(준공시점)을 사용자에게 입력 받아 해당 시점 신규 분양권 포함 총 주택 수를 계산
         */
        public CalculationBuyResultResponse branchNo004(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo004 - 취득세 분기번호 004 : 분양권 준공시점(잔금지급일) 총 주택 수");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "004")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // (취득주택 포함)보유주택 수
            long ownHouseCount = getOwnHouseCount(calculationBuyResultRequest);

            // 1주택
            if(ownHouseCount == 1){
                selectNo = 1;
            }
            // 2주택
            else if(ownHouseCount == 2){
                selectNo = 2;
            }
            // 3주택
            else if(ownHouseCount == 3){
                selectNo = 3;
            }
            // 4주택 이상
            else if(ownHouseCount >= 4){
                selectNo = 4;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 005
         * 분기명 : 준공시점 기준 조정대상지역여부
         * 분기설명 : 잔금지급일(준공시점)을 사용자에게 입력 받아 해당 시점 신규 분양권이 조정대상지역 해당 여부를 확인
         */
        public CalculationBuyResultResponse branchNo005(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo005 - 취득세 분기번호 005 : 준공시점 기준 조정대상지역여부");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "005")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 조정대상지역여부
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(calculationBuyResultRequest.getRoadAddr()));

            if(isAdjustmentTargetArea){
                selectNo = 1;
            }else{
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 006
         * 분기명 : 준공시점 기준 조정대상지역여부
         * 분기설명 : 잔금지급일(준공시점)을 사용자에게 입력 받아 해당 시점 신규 분양권이 조정대상지역 해당 여부를 확인
         */
        public CalculationBuyResultResponse branchNo006(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo006 - 취득세 분기번호 006 : 준공시점 기준 조정대상지역여부");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "006")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 조정대상지역여부
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(calculationBuyResultRequest.getRoadAddr()));

            if(isAdjustmentTargetArea){
                selectNo = 1;
            }else{
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 007
         * 분기명 : 주택멸실여부
         * 분기설명 : 주택멸실여부를 사용자에게 입력 받아 주택멸실여부 확인
         */
        public CalculationBuyResultResponse branchNo007(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo007 - 취득세 분기번호 007 : 주택멸실여부");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "006")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 주택멸실여부
            Boolean isDestruction = calculationBuyResultRequest.getIsDestruction();

            if(isDestruction){
                selectNo = 1;
            }else{
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 008
         * 분기명 : 취득세 중과 배제 주택 해당 여부
         * 분기설명 : 공시지가 1억원 이하인 경우 취득세 중과 배제 주택 해당으로 처리(추후 확장 계획)
         */
        public CalculationBuyResultResponse branchNo008(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo008 - 취득세 분기번호 008 : 취득세 중과 배제 주택 해당 여부");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            // 공시지가
            Boolean isPubLandPriceOver100Mil = calculationBuyResultRequest.getIsPubLandPriceOver100Mil();
            log.info("isPubLandPriceOver100Mil : " + isPubLandPriceOver100Mil);
            Long pubLandPrice = calculationBuyResultRequest.getPubLandPrice();

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "008")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 공시지가 1억 이하
            if(!isPubLandPriceOver100Mil){
                selectNo = 1;
            }
            // 공시지가 1억 초과
            else{
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            /*for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PRICE, Long.toString(pubLandPrice), variableData, dataMethod)){
                    selectNo = calculationProcess.getCalculationProcessId().getSelectNo();
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());

                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                }
            }*/

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 009
         * 분기명 : 취득분양권 포함 보유주택 수
         * 분기설명 : 분양권 취득(매수)시점 취득 분양권을 포함한 주택 수
         */
        public CalculationBuyResultResponse branchNo009(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo009 - 취득세 분기번호 009 : 취득분양권 포함 보유주택 수");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "009")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // (취득분양권 포함)보유주택 수
            long ownHouseCount = getOwnHouseCount(calculationBuyResultRequest);

            // 1주택
            if(ownHouseCount == 1){
                selectNo = 1;
            }
            // 2주택
            else if(ownHouseCount == 2){
                selectNo = 2;
            }
            // 3주택
            else if(ownHouseCount == 3){
                selectNo = 3;
            }
            // 4주택 이상
            else if(ownHouseCount >= 4){
                selectNo = 4;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 010
         * 분기명 : 취득일 기준 조정대상지역여부
         * 분기설명 : 준공후분양권은 분양권 취득일이 기준(일반주택처럼 소유권이전 시점이 아님)
         */
        public CalculationBuyResultResponse branchNo010(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo010 - 취득세 분기번호 010 : 취득일 기준 조정대상지역여부");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "010")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 조정대상지역여부
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(calculationBuyResultRequest.getRoadAddr()));

            if(isAdjustmentTargetArea){
                selectNo = 1;
            }else{
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 011
         * 분기명 : 종전주택이 분양권이나 입주권인 경우
         * 분기설명 : 종전주택이 2020.08.12 이후 취득한 분양권이나 지방세법상 입주권인 경우(재개발, 재건축, 소규모재건축)
         */
        public CalculationBuyResultResponse branchNo011(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo011 - 취득세 분기번호 011 : 종전주택이 분양권이나 입주권인 경우");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            String houseType = EMPTY;
            String buyDate = EMPTY;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "011")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 보유주택 수를 직접 입력한 경우
            if(calculationBuyResultRequest.getIsOwnHouseCntRegist()){
                selectNo = 1;
            }
            // 보유주택 정보를 조회하여 가져오는 경우
            else{
                // 종전주택이 2020.08.12 이후 취득한 분양권이나 지방세법상 입주권인 경우(재개발, 재건축, 소규모재건축)
                House lastOwnHouse = getLastOwnHouse();

                if(calculationBuyResultRequest != null && calculationBuyResultRequest.getBuyDate() != null){
                    buyDate = calculationBuyResultRequest.getBuyDate().toString().replace(HYPHEN, EMPTY);
                }

                if(lastOwnHouse != null){
                    houseType = lastOwnHouse.getHouseType();

                    // 일반 주택인 경우(3:입주권, 5:분양권)
                    if(!THREE.equals(houseType) && !FIVE.equals(houseType)){
                        selectNo = 1;
                    }
                }
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo != 0){
                    if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                        log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());

                        if(calculationProcess.isHasNextBranch()){
                            nextBranchNo = calculationProcess.getNextBranchNo();
                            hasNext = true;
                        }else{
                            taxRateCode = calculationProcess.getTaxRateCode();
                            dedCode = calculationProcess.getDedCode();
                        }
                        break;
                    }
                }else{
                    String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                    String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                    if(checkSelectNoCondition(DATA_TYPE_DATE, buyDate, variableData, dataMethod)){
                        selectNo = calculationProcess.getCalculationProcessId().getSelectNo();
                        log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());

                        if(calculationProcess.isHasNextBranch()){
                            nextBranchNo = calculationProcess.getNextBranchNo();
                            hasNext = true;
                        }else{
                            taxRateCode = calculationProcess.getTaxRateCode();
                            dedCode = calculationProcess.getDedCode();
                        }
                        break;
                    }
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 012
         * 분기명 : 분양권/입주권 완공 후 3년 이내 종전 또는 신규(신축)주택 양도예정
         * 분기설명 : 분양권/입주권이 완공된 후 3년 이내에 종전주택 또는 신규(신축)주택을 양도 예정
         */
        public CalculationBuyResultResponse branchNo012(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo012 - 취득세 분기번호 012 : 분양권/입주권 완공 후 3년 이내 종전 또는 신규(신축)주택 양도예정");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "012")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // (사용자 입력)완공 후 n년 이내 신규주택 양도 예정 여부
            Boolean hasSellPlan = calculationBuyResultRequest.getHasSellPlan();

            if(hasSellPlan){
                selectNo = 1;
            }else{
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 013
         * 분기명 : 취득일 기준 조정대상지역여부
         * 분기설명 : 취득일 기준 조정대상지역여부
         */
        public CalculationBuyResultResponse branchNo013(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo013 - 취득세 분기번호 013 : 취득일 기준 조정대상지역여부");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "013")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 조정대상지역여부
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(calculationBuyResultRequest.getRoadAddr()));

            if(isAdjustmentTargetArea){
                selectNo = 1;
            }else{
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 014
         * 분기명 : 신규주택 포함 보유주택 수
         * 분기설명 : 신규 취득하는 주택을 포함한 총 보유주택 수
         */
        public CalculationBuyResultResponse branchNo014(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo014 - 취득세 분기번호 014 : 신규주택 포함 보유주택 수");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "014")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // (취득주택 포함)보유주택 수
            long ownHouseCount = getOwnHouseCount(calculationBuyResultRequest);

            if(ownHouseCount == 1){
                selectNo = 1;
            }else if(ownHouseCount == 2){
                selectNo = 2;
            }else if(ownHouseCount == 3){
                selectNo = 3;
            }else if(ownHouseCount >= 4){
                selectNo = 4;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 015
         * 분기명 : 종전주택이 분양권이나 입주권인 경우
         * 분기설명 : 종전주택이 2020.08.12 이후 취득한 분양권이나 지방세법상 입주권인 경우(재개발, 재건축, 소규모재건축)
         */
        public CalculationBuyResultResponse branchNo015(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo015 - 취득세 분기번호 015 : 종전주택이 분양권이나 입주권인 경우");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            String houseType = EMPTY;
            String buyDate = EMPTY;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "015")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 보유주택 수를 직접 입력한 경우
            if(calculationBuyResultRequest.getIsOwnHouseCntRegist()){
                log.info("보유주택 수 직접 입력에 해당");
                selectNo = 1;
            }
            // 보유주택 정보를 조회하여 가져오는 경우
            else{
                // 종전주택이 2020.08.12 이후 취득한 분양권이나 지방세법상 입주권인 경우(재개발, 재건축, 소규모재건축)
                House lastOwnHouse = getLastOwnHouse();

                if(calculationBuyResultRequest != null && calculationBuyResultRequest.getBuyDate() != null){
                    buyDate = calculationBuyResultRequest.getBuyDate().toString().replace(HYPHEN, EMPTY);
                }

                if(lastOwnHouse != null){
                    houseType = lastOwnHouse.getHouseType();

                    // 일반 주택인 경우(3:입주권, 5:분양권)
                    if(!THREE.equals(houseType) && !FIVE.equals(houseType)){
                        log.info("종전주택이 일반주택인 경우(분양권이나 입주권이 아닌 경우)에 해당");
                        selectNo = 1;
                    }
                }
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo != 0){
                    if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                        log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());

                        if(calculationProcess.isHasNextBranch()){
                            nextBranchNo = calculationProcess.getNextBranchNo();
                            hasNext = true;
                        }else{
                            taxRateCode = calculationProcess.getTaxRateCode();
                            dedCode = calculationProcess.getDedCode();
                        }
                        break;
                    }
                }else{
                    String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                    String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                    if(checkSelectNoCondition(DATA_TYPE_DATE, buyDate, variableData, dataMethod)){
                        selectNo = calculationProcess.getCalculationProcessId().getSelectNo();
                        log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());

                        if(calculationProcess.isHasNextBranch()){
                            nextBranchNo = calculationProcess.getNextBranchNo();
                            hasNext = true;
                        }else{
                            taxRateCode = calculationProcess.getTaxRateCode();
                            dedCode = calculationProcess.getDedCode();
                        }
                        break;
                    }
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 016
         * 분기명 : 종전주택 완공 후 3년 이내에 종전(신축) 또는 신규 주택 양도예정
         * 분기설명 : 종전주택이 완공된 후 3년 이내에 종전(신축)주택 또는 신규주택을 양도 예정
         */
        public CalculationBuyResultResponse branchNo016(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo016 - 취득세 분기번호 016 : 분양권/입주권 완공 후 3년 이내 종전 또는 신규(신축)주택 양도예정");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "016")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // (사용자 입력)완공 후 n년 이내 종전 또는 신규 주택 양도 예정 여부
            Boolean hasSellPlan = calculationBuyResultRequest.getHasSellPlan();

            if(hasSellPlan){
                selectNo = 1;
            }else{
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 017
         * 분기명 : 신규주택 취득시점 조정지역 여부
         * 분기설명 : 신규주택 취득시점 조정지역 여부
         */
        public CalculationBuyResultResponse branchNo017(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo017 - 취득세 분기번호 017 : 신규주택 취득시점 조정지역 여부");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "017")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 조정대상지역여부
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(calculationBuyResultRequest.getRoadAddr()));

            if(isAdjustmentTargetArea){
                selectNo = 1;
            }else{
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 018
         * 분기명 : 신규주택 취득시점 조정지역 여부
         * 분기설명 : 신규주택 취득시점 조정지역 여부
         */
        public CalculationBuyResultResponse branchNo018(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo018 - 취득세 분기번호 018 : 신규주택 취득시점 조정지역 여부");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "018")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 조정대상지역여부
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(calculationBuyResultRequest.getRoadAddr()));

            if(isAdjustmentTargetArea){
                selectNo = 1;
            }else{
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 019
         * 분기명 : 종전주택을 신규주택 취득일 기준 3년 내 양도예정 여부
         * 분기설명 : 종전주택을 신규주택 취득일 기준 3년 내 양도예정
         */
        public CalculationBuyResultResponse branchNo019(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo019 - 취득세 분기번호 019 : 종전주택을 신규주택 취득일 기준 3년 내 양도예정 여부");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "019")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // (사용자 입력)취득후 n년 이내 종전주택 양도 에정 여부
            Boolean hasSellPlan = calculationBuyResultRequest.getHasSellPlan();

            if(hasSellPlan){
                selectNo = 1;
            }else{
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        /**
         * 분기번호 : 020
         * 분기명 : 신규주택 취득시점 조정지역 여부
         * 분기설명 : 신규주택 취득시점 조정지역 여부
         */
        public CalculationBuyResultResponse branchNo020(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch branchNo020 - 취득세 분기번호 020 : 신규주택 취득시점 조정지역 여부");

            CalculationBuyResultResponse calculationBuyResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "020")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 조정대상지역여부
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(calculationBuyResultRequest.getRoadAddr()));

            if(isAdjustmentTargetArea){
                selectNo = 1;
            }else{
                selectNo = 2;
            }

            for(CalculationProcess calculationProcess : list){
                if(selectNo == calculationProcess.getCalculationProcessId().getSelectNo()){
                    log.info("selectNo : " + selectNo + ", selectContent : " + calculationProcess.getSelectContent());
                    if(calculationProcess.isHasNextBranch()){
                        nextBranchNo = calculationProcess.getNextBranchNo();
                        hasNext = true;
                    }else{
                        taxRateCode = calculationProcess.getTaxRateCode();
                        dedCode = calculationProcess.getDedCode();
                    }
                    break;
                }
            }

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationBuyResultRequest.class);
                    calculationBuyResultResponse = (CalculationBuyResultResponse) method.invoke(target, calculationBuyResultRequest);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED);
                }
            }else{
                calculationBuyResultResponse = getCalculationBuyResultResponse(calculationBuyResultRequest, taxRateCode, dedCode);
            }

            return calculationBuyResultResponse;
        }

        // 취득세 계산 결과 조회
        private CalculationBuyResultResponse getCalculationBuyResultResponse(CalculationBuyResultRequest calculationBuyResultRequest, String taxRateCode, String dedCode){
            log.info(">>> CalculationBranch getCalculationBuyResult - 취득세 계산 수행");

            double buyTaxRate = 0;      // 취득세율
            double taxRate1 = 0;        // 세율1
            double taxRate2 = 0;        // 세율2
            double addTaxRate1 = 0;     // 추가세율1
            double addTaxRate2 = 0;     // 추가세율2
            double finalTaxRate1 = 0;   // 최종세율1
            double finalTaxRate2 = 0;   // 최종세율2
            double agrTaxRate = 0;      // 농어촌특별세
            double eduTaxRate = 0;      // 지방교육세

            long buyTaxPrice = 0;       // 취득세액
            long agrTaxPrice = 0;       // 농어촌특별세액
            long eduTaxPrice = 0;       // 지방교육세액
            long totalTaxPrice = 0;     // 총납부세액

            // (취득주택 포함)보유주택 수
            long ownHouseCount = getOwnHouseCount(calculationBuyResultRequest);

            // 취득가액
            long buyPrice = calculationBuyResultRequest.getBuyPrice();

            // (취득주택)조정지역여부
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(calculationBuyResultRequest.getRoadAddr()));

            log.info("- 취득주택 포함 보유주택 수 : " + ownHouseCount);
            log.info("- 취득가액 : " + buyPrice);
            log.info("- 취득주택 조정지역여부 : " + isAdjustmentTargetArea);
            
            // 세율정보
            TaxRateInfo taxRateInfo = null;
            if(taxRateCode != null && !taxRateCode.isBlank()){
                log.info("세율정보 조회");
                taxRateInfo = taxRateInfoRepository.findByTaxRateCode(taxRateCode)
                        .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "세율 정보를 가져오는 중 오류가 발생했습니다."));
            }

            // 공제정보
            DeductionInfo deductionInfo = null;
            if(dedCode != null && !dedCode.isBlank()){
                log.info("공제정보 조회");
                deductionInfo = deductionInfoRepository.findByDedCode(dedCode)
                        .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "공제 정보를 가져오는 중 오류가 발생했습니다."));
            }

            /* 1. 취득세 계산 */
            log.info("1. 취득세 계산");
            if(taxRateInfo != null){
                // 세율이 상수인 경우
                if(YES.equals(taxRateInfo.getConstYn())){
                    buyTaxRate = Double.parseDouble(StringUtils.defaultString(taxRateInfo.getTaxRate1(), ZERO));
                    buyTaxPrice = (long) (buyPrice * buyTaxRate);
                }
                // 세율이 상수가 아닌 경우(변수)
                else{
                    if(taxRateInfo.getTaxRate1() != null && !taxRateInfo.getTaxRate1().isBlank()){
                        // 세율이 2개인 경우
                        if(taxRateInfo.getTaxRate2() != null && !taxRateInfo.getTaxRate2().isBlank()){
                            // 세율1
                            if(GENERAL_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                                taxRate1 = calculateGeneralTaxRate(calculationBuyResultRequest, ownHouseCount, buyPrice, isAdjustmentTargetArea);
                            }else if(NONE_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                                taxRate1 = 0;
                            }else{
                                taxRate1 = Double.parseDouble(StringUtils.defaultString(taxRateInfo.getTaxRate1(), ZERO));
                            }

                            // 세율2
                            if(GENERAL_TAX_RATE.equals(taxRateInfo.getTaxRate2())){
                                taxRate2 = calculateGeneralTaxRate(calculationBuyResultRequest, ownHouseCount, buyPrice, isAdjustmentTargetArea);
                            }else if(NONE_TAX_RATE.equals(taxRateInfo.getTaxRate2())){
                                taxRate2 = 0;
                            }else{
                                taxRate2 = Double.parseDouble(StringUtils.defaultString(taxRateInfo.getTaxRate2(), ZERO));
                            }

                            // 추가세율1
                            if(taxRateInfo.getAddTaxRate1() != null && !taxRateInfo.getAddTaxRate1().isBlank()){
                                addTaxRate1 = Double.parseDouble(StringUtils.defaultString(taxRateInfo.getAddTaxRate1(), ZERO));
                            }

                            // 추가세율2
                            if(taxRateInfo.getAddTaxRate2() != null && !taxRateInfo.getAddTaxRate2().isBlank()){
                                addTaxRate2 = Double.parseDouble(StringUtils.defaultString(taxRateInfo.getAddTaxRate2(), ZERO));
                            }

                            finalTaxRate1 = taxRate1 + addTaxRate1;
                            finalTaxRate2 = taxRate2 + addTaxRate2;

                            // 사용함수
                            String usedFunc = StringUtils.defaultString(taxRateInfo.getUsedFunc());

                            // MAX : 세율1과 세율2 중 최대값 사용
                            if(MAX.equals(usedFunc)){
                                buyTaxRate = Math.max(finalTaxRate1, finalTaxRate2);
                                buyTaxPrice = (long) (buyPrice * buyTaxRate);
                            }
                            // OR_LESS_MORE : 기준금액 이하 세율1, 기준금액 초과 세율2
                            else if(OR_LESS_MORE.equals(usedFunc)){
                                long basePrice = taxRateInfo.getBasePrice();

                                if(buyPrice > basePrice){
                                    buyTaxPrice = (long)((basePrice * finalTaxRate1) + ((buyPrice - basePrice) * finalTaxRate2));
                                }else{
                                    buyTaxPrice = (long)(buyPrice * finalTaxRate1);
                                }

                                buyTaxRate = (double)(buyTaxPrice / buyPrice); // 취득세액으로 취득세율을 계산
                            }
                        }
                        // 세율이 1개인 경우
                        else{
                            // 일반과세(일반세율)
                            if(GENERAL_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                                taxRate1 = calculateGeneralTaxRate(calculationBuyResultRequest, ownHouseCount, buyPrice, isAdjustmentTargetArea);
                            }
                            // 비과세
                            else if(NONE_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                                taxRate1 = 0;
                            }

                            // 추가세율
                            if(taxRateInfo.getAddTaxRate1() != null && !taxRateInfo.getAddTaxRate1().isBlank()){
                                addTaxRate1 = Double.parseDouble(StringUtils.defaultString(taxRateInfo.getAddTaxRate1(), ZERO));
                            }

                            finalTaxRate1 = taxRate1 + addTaxRate1;
                            buyTaxRate = finalTaxRate1;
                            buyTaxPrice = (long) (buyPrice * buyTaxRate);
                        }
                    }
                }
            }

            /* 2. 농어촌특별세 계산 */
            log.info("2. 농어촌특별세 계산");
            // 전용면적 85제곱미터 초과만 농어촌특별세 대상
            Boolean isAreaOver85 = calculationBuyResultRequest.getIsAreaOver85();
            //double area = calculationBuyResultRequest.getArea();
            //if(area > AREA_85){
            if(isAreaOver85){
                // 1주택
                if(ownHouseCount == 1){
                    agrTaxRate = 0.002;        // 농어촌특별세율 : 0.2%
                }
                // 2주택
                else if(ownHouseCount == 2){
                    // 조정대상지역
                    if(isAdjustmentTargetArea){
                        agrTaxRate = 0.006;    // 농어촌특별세율 : 0.6%
                    }else{
                        agrTaxRate = 0.002;    // 농어촌특별세율 : 0.2%
                    }
                }
                // 3주택
                else if(ownHouseCount == 3){
                    // 조정대상지역
                    if(isAdjustmentTargetArea){
                        agrTaxRate = 0.01;     // 농어촌특별세율 : 1%
                    }else{
                        agrTaxRate = 0.006;    // 농어촌특별세율 : 0.6%
                    }
                }
                // 4주택 이상
                else if(ownHouseCount >= 4){
                    // 조정대상지역
                    if(isAdjustmentTargetArea){
                        agrTaxRate = 0.01;     // 농어촌특별세율 : 1%
                    }else{
                        agrTaxRate = 0.006;    // 농어촌특별세율 : 0.6%
                    }
                }
            }

            // 농어촌특별세액
            agrTaxPrice = (long)(buyPrice * agrTaxRate);

            /* 3. 지방교육세 계산 */
            log.info("3. 지방교육세 계산");
            // 1주택
            if(ownHouseCount == 1){
                // 6억 이하
                if(buyPrice <= SIX_HND_MIL){
                    eduTaxRate = 0.001;                // 지방교육세율 : 0.1%
                }
                // 6억 초과, 9억 이하
                else if(buyPrice > SIX_HND_MIL && buyPrice <= NINE_HND_MIL){
                    eduTaxRate = buyTaxRate / 10;       // 지방교육세율 : 취득세율의 1/10
                }
                // 9억 초과
                else{
                    eduTaxRate = 0.003;                // 지방교육세율 : 0.3%
                }
            }
            // 2주택
            else if(ownHouseCount == 2){
                // 조정대상지역
                if(isAdjustmentTargetArea){
                    eduTaxRate = 0.004;                // 지방교육세율 : 0.4%
                }else{
                    // 6억 이하
                    if(buyPrice <= SIX_HND_MIL){
                        eduTaxRate = 0.001;            // 지방교육세율 : 0.1%
                    }
                    // 6억 초과, 9억 이하
                    else if(buyPrice > SIX_HND_MIL && buyPrice <= NINE_HND_MIL){
                        eduTaxRate = buyTaxRate / 10;   // 지방교육세율 : 취득세율의 1/10
                    }
                    // 9억 초과
                    else{
                        eduTaxRate = 0.003;            // 지방교육세율 : 0.3%
                    }
                }
            }
            // 3주택
            else if(ownHouseCount == 3){
                // 조정대상지역
                if(isAdjustmentTargetArea){
                    eduTaxRate = 0.004;                // 지방교육세율 : 0.4%
                }else{
                    eduTaxRate = 0.004;                // 지방교육세율 : 0.4%
                }
            }
            // 4주택 이상
            else if(ownHouseCount >= 4){
                // 조정대상지역
                if(isAdjustmentTargetArea){
                    eduTaxRate = 0.004;                // 지방교육세율 : 0.4%
                }else{
                    eduTaxRate = 0.004;                // 지방교육세율 : 0.4%
                }
            }

            // 지방교육세액
            eduTaxPrice = (long)(buyPrice * eduTaxRate);

            /* 4. 총납부세액 계산 */
            log.info("4. 총납부세액 계산");
            totalTaxPrice = buyTaxPrice + eduTaxPrice + agrTaxPrice;

            // 취득세 계산 결과 세팅
            List<CalculationBuyOneResult> calculationBuyResultOneList = new ArrayList<>();

            int ownerCount = calculationBuyResultRequest.getOwnerCnt();
            double userProportion = (double)calculationBuyResultRequest.getUserProportion() / 100;
            double restProPortion = 1 - userProportion;

            for(int i=0; i<ownerCount; i++){
                double proportion = 1;
                if(ownerCount > 1){
                    if(i == 0) proportion = userProportion;
                    else proportion = restProPortion;
                }

                String buyPriceStr = String.format("%.0f", buyPrice*proportion);
                String buyTaxRateStr = String.format("%.2f", buyTaxRate*proportion*100);
                String buyTaxPriceStr = String.format("%.0f", buyTaxPrice*proportion);
                String eduTaxRateStr = String.format("%.2f", eduTaxRate*proportion*100);
                String eduTaxPriceStr = String.format("%.0f", eduTaxPrice*proportion);
                String agrTaxRateStr = String.format("%.2f", agrTaxRate*proportion*100);
                String agrTaxPriceStr = String.format("%.0f", agrTaxPrice*proportion);
                String totalTaxPriceStr = String.format("%.0f", totalTaxPrice*proportion);

                calculationBuyResultOneList.add(
                        CalculationBuyOneResult.builder()
                                .buyPrice(buyPriceStr)
                                .buyTaxRate(buyTaxRateStr)
                                .buyTaxPrice(buyTaxPriceStr)
                                .eduTaxRate(eduTaxRateStr)
                                .eduTaxPrice(eduTaxPriceStr)
                                .eduDiscountPrice(ZERO)
                                .agrTaxRate(agrTaxRateStr)
                                .agrTaxPrice(agrTaxPriceStr)
                                .totalTaxPrice(totalTaxPriceStr)
                                .build());
            }

            return CalculationBuyResultResponse.builder()
                    .listCnt(ownerCount)
                    .list(calculationBuyResultOneList)
                    .build();
        }

        // (취득주택 포함)보유주택 수 가져오기
        private long getOwnHouseCount(CalculationBuyResultRequest calculationBuyResultRequest){
            log.info(">>> CalculationBranch getOwnHouseCount - (취득주택 포함)보유주택 수 가져오기");

            if(calculationBuyResultRequest.getIsOwnHouseCntRegist()){
                log.info("보유주택 수 직접입력을 통한 보유주택 수 가져오기");
                return calculationBuyResultRequest.getOwnHouseCnt() + 1;    // 취득주택 포함이므로 +1
            }else{
                log.info("보유주택 수 조회(청약홈)를 통한 보유주택 수 가져오기");
                User findUser = userUtil.findCurrentUser(); // 호출 사용자 조회
                return houseRepository.countByUserId(findUser.getId()) + 1; // 취득주택 포함이므로 +1
            }
        }

        // 종전주택 가져오기
        private House getLastOwnHouse(){
            List<House> houseList = houseRepository.findByUserId(userUtil.findCurrentUser().getId()).orElse(null);
            House lastOwnHouse = null;
            LocalDate lastBuyDate = null;

            if(houseList != null && !houseList.isEmpty()){
                for(House house : houseList){
                    LocalDate buyDate = house.getBuyDate();
                    if(buyDate == null){
                        lastBuyDate = buyDate;
                        lastOwnHouse = house;
                    }else{
                        if(buyDate.isAfter(lastBuyDate)){
                            lastBuyDate = buyDate;
                            lastOwnHouse = house;
                        }
                    }
                }
            }

            return lastOwnHouse;
        }

        // (취득세)일반과세(일반세율) 계산
        private double calculateGeneralTaxRate(CalculationBuyResultRequest calculationBuyResultRequest, long ownHouseCount, long buyPrice, boolean isAdjustmentTargetArea){
            double taxRate = 0;

            // 1주택
            if(ownHouseCount == 1){
                // 6억 이하
                if(buyPrice <= SIX_HND_MIL){
                    // 취득세율 : 1%
                    taxRate = 0.01;
                }
                // 6억 초과, 9억 이하
                else if(buyPrice > SIX_HND_MIL && buyPrice <= NINE_HND_MIL){
                    // 취득세율 : (((취득가액 / 1억) x 2 / 3 - 3) x 1)%
                    taxRate = (double)((buyPrice / ONE_HND_MIL) * 2 / 3 - 3) / 100;
                }
                // 9억 초과
                else{
                    // 취득세율 : 3%
                    taxRate = 0.03;
                }
            }
            // 2주택
            else if(ownHouseCount == 2){
                // 조정대상지역 외(조정대상지역에 대한 취득세율은 다른 프로세스로 접근)
                if(!isAdjustmentTargetArea){
                    // 6억 이하
                    if(buyPrice <= SIX_HND_MIL){
                        // 취득세율 : 1%
                        taxRate = 0.01;
                    }
                    // 6억 초과, 9억 이하
                    else if(buyPrice > SIX_HND_MIL && buyPrice <= NINE_HND_MIL){
                        // 취득세율 : (((취득가액 / 1억) x 2 / 3 - 3) x 1)%
                        taxRate = (double)((buyPrice / ONE_HND_MIL) * 2 / 3 - 3) / 100;
                    }
                    // 9억 초과
                    else{
                        // 취득세율 : 3%
                        taxRate = 0.03;
                    }
                }
            }

            return taxRate;
        }

        // 조정대상지역 체크
        private boolean checkAdjustmentTargetArea(String address){
            String siGunGu = houseAddressService.separateAddress(address).getSiGunGu();

            // 조정대상지역(용산구, 서초구, 강남구, 송파구)
            return ADJUSTMENT_TARGET_AREA1.equals(siGunGu) || ADJUSTMENT_TARGET_AREA2.equals(siGunGu) || ADJUSTMENT_TARGET_AREA3.equals(siGunGu) || ADJUSTMENT_TARGET_AREA4.equals(siGunGu);
        }

        // selectNo 조건 부합 여부 체크
        private boolean checkSelectNoCondition(int dataType, String inputData, String variableData, String dataMethod){
            boolean result = false;
            // 금액
            if(DATA_TYPE_PRICE == dataType){
                if(inputData == null || variableData == null || dataMethod == null){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 계산을 위한 파라미터가 올바르지 않습니다.");
                }
                long inputPrice = Long.parseLong(StringUtils.defaultString(inputData, ZERO));
                long variablePrice = Long.parseLong(StringUtils.defaultString(variableData, ZERO));

                // 미만
                if(LESS.equals(dataMethod)){
                    if(inputPrice < variablePrice){
                        result = true;
                    }
                }
                // 이하
                else if(OR_LESS.equals(dataMethod)){
                    if(inputPrice <= variablePrice){
                        result = true;
                    }
                }
                // 초과
                else if(MORE.equals(dataMethod)){
                    if(inputPrice > variablePrice){
                        result = true;
                    }
                }
                // 이상
                else if(OR_MORE.equals(dataMethod)){
                    if(inputPrice >= variablePrice){
                        result = true;
                    }
                }
            }
            // 날짜
            else if(DATA_TYPE_DATE == dataType){
                if(inputData == null || variableData == null || dataMethod == null){
                    throw new CustomException(ErrorCode.CALCULATION_BUY_TAX_FAILED, "취득세 계산을 위한 파라미터가 올바르지 않습니다.");
                }

                LocalDate inputDate = LocalDate.parse(inputData, DateTimeFormatter.ofPattern("yyyyMMdd"));
                String[] variableDataArr = variableData.split(",");
                LocalDate variableDate1 = null;
                LocalDate variableDate2 = null;

                variableDate1 = LocalDate.parse(variableDataArr[0], DateTimeFormatter.ofPattern("yyyyMMdd"));
                if(variableDataArr.length > 1){
                    variableDate2 = LocalDate.parse(variableDataArr[1], DateTimeFormatter.ofPattern("yyyyMMdd"));
                }

                // YYYYMMDD일 이전
                if(BEFORE.equals(dataMethod)){
                    if(inputDate.isBefore(variableDate1)){
                        result = true;
                    }
                }
                // YYYYYMMDD일 포함 이전
                else if(OR_BEFORE.equals(dataMethod)){
                    if(inputDate.isBefore(variableDate1) || inputDate.isEqual(variableDate1)){
                        result = true;
                    }
                }
                // YYYYMMDD일 이후
                else if(AFTER.equals(dataMethod)){
                    if(inputDate.isAfter(variableDate1)){
                        result = true;
                    }
                }
                // YYYYMMDD일 포함 이후
                else if(OR_AFTER.equals(dataMethod)){
                    if(inputDate.isAfter(variableDate1) || inputDate.isEqual(variableDate1)){
                        result = true;
                    }
                }
                // YYYYMMDD일 부터 YYYYMMDD일 까지
                else if(FROM_TO.equals(dataMethod)){
                    if(variableDate2 != null){
                        if((inputDate.isAfter(variableDate1) || inputDate.isEqual(variableDate1)) && (inputDate.isBefore(variableDate2) || inputDate.isEqual(variableDate2))){
                            result = true;
                        }
                    }
                }
            }

            return result;
        }
    }
}
