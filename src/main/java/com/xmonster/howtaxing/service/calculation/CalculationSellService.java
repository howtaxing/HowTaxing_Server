package com.xmonster.howtaxing.service.calculation;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.calculation.*;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultResponse.CalculationSellOneResult;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.house.HouseAddressDto;
import com.xmonster.howtaxing.model.*;
import com.xmonster.howtaxing.repository.adjustment_target_area.AdjustmentTargetAreaRepository;
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
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CalculationSellService {
    private final HouseAddressService houseAddressService;

    private final CalculationProcessRepository calculationProcessRepository;
    private final TaxRateInfoRepository taxRateInfoRepository;
    private final DeductionInfoRepository deductionInfoRepository;
    private final HouseRepository houseRepository;
    private final AdjustmentTargetAreaRepository adjustmentTargetAreaRepository;
    private final CalculationHistoryRepository calculationHistoryRepository;
    private final CalculationSellRequestHistoryRepository calculationSellRequestHistoryRepository;
    private final CalculationAdditionalAnswerRequestHistoryRepository calculationAdditionalAnswerRequestHistoryRepository;
    private final CalculationSellResponseHistoryRepository calculationSellResponseHistoryRepository;
    private final CalculationCommentaryResponseHistoryRepository calculationCommentaryResponseHistoryRepository;
    private final CalculationOwnHouseHistoryRepository calculationOwnHouseHistoryRepository;
    private final CalculationOwnHouseHistoryDetailRepository calculationOwnHouseHistoryDetailRepository;

    private final UserUtil userUtil;
    private final HouseUtil houseUtil;

    private Class<?> calculationBranchClass;
    private CalculationBranch target;

    // 양도소득세 계산 결과 조회
    public Object getCalculationSellResult(CalculationSellResultRequest calculationSellResultRequest){
        log.info(">> [Service]CalculationSellService getCalculationSellResult - 양도소득세 계산 결과 조회");

        // 요청 데이터 유효성 검증
        validationCheckRequestData(calculationSellResultRequest);

        log.info("(Calculation)양도소득세 계산 결과 조회 요청 : " + calculationSellResultRequest.toString());

        // 양도주택정보
        House house = houseRepository.findByHouseId(calculationSellResultRequest.getHouseId())
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR));

        // 분기 메소드
        try{
            calculationBranchClass = Class.forName("com.xmonster.howtaxing.service.calculation.CalculationSellService$CalculationBranch");
            target = new CalculationBranch();

            Method method = calculationBranchClass.getMethod("calculationStart", CalculationSellResultRequest.class, House.class);

            Object result = method.invoke(target, calculationSellResultRequest, house);

            if(result != null) log.info("(Calculation)양도소득세 계산 결과 조회 응답 : " + result.toString());

            return ApiResponse.success(result);

        }catch(Exception e){
            throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
        }
    }

    // 요청 데이터 유효성 검증
    private void validationCheckRequestData(CalculationSellResultRequest calculationSellResultRequest){
        log.info(">>> CalculationBranch validationCheck - 요청 데이터 유효성 검증");

        if(calculationSellResultRequest == null) throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도주택 정보가 입력되지 않았습니다.");

        Long houseId = calculationSellResultRequest.getHouseId();
        LocalDate sellContractDate = calculationSellResultRequest.getSellContractDate();
        LocalDate sellDate = calculationSellResultRequest.getSellDate();
        Long sellPrice = calculationSellResultRequest.getSellPrice();
        Long necExpensePrice = calculationSellResultRequest.getNecExpensePrice();
        Integer ownerCnt = calculationSellResultRequest.getOwnerCnt();
        Integer userProportion = calculationSellResultRequest.getUserProportion();

        if(houseId == null){
            throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도주택의 주택ID 정보가 입력되지 않았습니다.");
        }

        if(sellContractDate == null){
            throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도주택의 계약일자 정보가 입력되지 않았습니다.");
        }

        if(sellDate == null){
            throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도주택의 양도일자 정보가 입력되지 않았습니다.");
        }

        if(sellPrice == null){
            throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도주택의 양도가액 정보가 입력되지 않았습니다.");
        }

        if(necExpensePrice == null){
            throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도주택의 필요경비금액 정보가 입력되지 않았습니다.");
        }

        if(ownerCnt == null){
            throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도주택의 소유자수 정보가 입력되지 않았습니다.");
        }

        if(userProportion == null){
            throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도주택의 본인지분비율 정보가 입력되지 않았습니다.");
        }
    }

    private class CalculationBranch {
        // Start
        public Object calculationStart(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBuyService calculationStart - 계산 시작");
            CalculationSellResultResponse calculationBuyResultResponse;

            try{
                Method method = calculationBranchClass.getMethod("branchNo001", CalculationSellResultRequest.class, House.class);
                calculationBuyResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
            }catch(Exception e){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
            }

            return calculationBuyResultResponse;
        }

        /*============================================================ 양도소득세 계산 프로세스 START ============================================================*/

        /**
         * 분기번호 : 001
         * 분기명 : 매도하려는 물건의 종류
         * 분기설명 : 매도하려는 물건의 종류(주택, 오피스텔)
         */
        public CalculationSellResultResponse branchNo001(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo001 - 양도소득세 분기번호 001 : 매도하려는 물건의 종류");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;
            
            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "001")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 현재 주택유형에 오피스텔이 없기 때문에 무조건 주택으로 세팅
            selectNo = 1;   // 주택(선택번호:1)

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
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            //calculationSellResultResponse = getCalculationBuyResultResponseTest();

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 002
         * 분기명 : 주택/분양권/입주권 구분
         * 분기설명 : 매도하려는 물건의 종류(주택, 분양권, 입주권)
         */
        public CalculationSellResultResponse branchNo002(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo002 - 양도소득세 분기번호 002 : 주택/분양권/입주권 구분");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "002")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(FIVE.equals(house.getHouseType())){
                selectNo = 2;   // 준공 분양권(선택번호:2)
            } else if(THREE.equals(house.getHouseType()) || house.getIsMoveInRight()){
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
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 003
         * 분기명 : 보유기간
         * 분기설명 : 양도주택(분양권)의 보유기간
         */
        public CalculationSellResultResponse branchNo003(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo003 - 양도소득세 분기번호 003 : 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "003")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String buyDate = (house.getBuyDate() != null) ? house.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("buyDate : " + buyDate);
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, buyDate, sellDate, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 004
         * 분기명 : 보유기간
         * 분기설명 : 양도주택(입주권)의 보유기간
         */
        public CalculationSellResultResponse branchNo004(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo004 - 양도소득세 분기번호 004 : 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "004")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String buyDate = (house.getBuyDate() != null) ? house.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("buyDate : " + buyDate);
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, buyDate, sellDate, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 007
         * 분기명 : 보유주택 수(매도주택 포함)
         * 분기설명 : 매도하려는 주택을 포함한 보유주택 수
         */
        public CalculationSellResultResponse branchNo007(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo007 - 양도소득세 분기번호 007 : 보유주택 수(매도주택 포함)");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "007")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 1주택
            if(houseUtil.countOwnHouse() == 1){
                selectNo = 1;
            }
            // 2주택
            else if(houseUtil.countOwnHouse() == 2){
                selectNo = 2;
            }
            // 3주택 이상
            else if(houseUtil.countOwnHouse() >= 3){
                selectNo = 3;
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
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 008
         * 분기명 : 취득일자 확인
         * 분기설명 : (1주택)취득일이 [2017.08.03] 이후 여부
         */
        public CalculationSellResultResponse branchNo008(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo008 - 양도소득세 분기번호 008 : 취득일자 확인");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "008")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String buyDate = (house.getBuyDate() != null) ? house.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("buyDate : " + buyDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_DATE, buyDate, EMPTY, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 009
         * 분기명 : 계약일자 확인
         * 분기설명 : (1주택)계약일이 [2017.08.03] 이후 여부
         */
        public CalculationSellResultResponse branchNo009(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo009 - 양도소득세 분기번호 009 : 계약일자 확인");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "009")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String contractDate = (house.getContractDate() != null) ? house.getContractDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("contractDate : " + contractDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_DATE, contractDate, EMPTY, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 010
         * 분기명 : 조정대상지역여부
         * 분기설명 : (1주택)취득일 기준 조정대상지역 기간해당 여부
         */
        public CalculationSellResultResponse branchNo010(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo010 - 양도소득세 분기번호 010 : 조정대상지역여부");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "010")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 조정대상지역여부(TODO. 취득일 기준으로 조정대상지역 체크하도록 수정)
            //boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(house.getRoadAddr()));
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(house.getJibunAddr()), house.getBuyDate());

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
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 011
         * 분기명 : 거주기간
         * 분기설명 : (1주택)양도주택의  거주기간
         */
        public CalculationSellResultResponse branchNo011(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo011 - 양도소득세 분기번호 011 : 거주기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "011")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            long stayPeriodYear = 0;
            long stayPeriodMonth = 0;

            List<CalculationAdditionalAnswerRequest> additionalAnswerList = calculationSellResultRequest.getAdditionalAnswerList();
            for(CalculationAdditionalAnswerRequest answer : additionalAnswerList){
                if(PERIOD_TYPE_DIAL.equals(answer.getQuestionId()) || PERIOD_TYPE_CERT.equals(answer.getQuestionId())){
                    Map<String, Object> stayPeriodMap = getStayPeriodYearAndMonth(answer.getAnswerValue());
                    if(stayPeriodMap.containsKey(STAY_PERIOD_YEAR)){
                        stayPeriodYear = (long)stayPeriodMap.get(STAY_PERIOD_YEAR);
                    }
                    if(stayPeriodMap.containsKey(STAY_PERIOD_MONTH)){
                        stayPeriodMonth = (long)stayPeriodMap.get(STAY_PERIOD_MONTH);
                    }
                    break;
                }
            }

            log.info("stayPeriodYear : " + stayPeriodYear + "년");
            log.info("stayPeriodMonth : " + stayPeriodMonth + "개월");

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, Long.toString(stayPeriodYear), Long.toString(stayPeriodMonth), variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 012
         * 분기명 : 상생임대인여부
         * 분기설명 : (1주택)상생임대인 여부
         */
        public CalculationSellResultResponse branchNo012(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo012 - 양도소득세 분기번호 012 : 상생임대인여부");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "012")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            List<CalculationAdditionalAnswerRequest> additionalAnswerList = calculationSellResultRequest.getAdditionalAnswerList();
            for(CalculationAdditionalAnswerRequest answer : additionalAnswerList){
                // 상생임대인여부
                if(Q_0006.equals(answer.getQuestionId())){
                    if(ANSWER_VALUE_01.equals(answer.getAnswerValue())){
                        selectNo = 1;
                    }else{
                        selectNo = 2;
                    }
                }
            }

            // 상생임대인여부
            /*boolean isWWLandLord = false;

            if(calculationSellResultRequest.getIsWWLandLord() != null){
                isWWLandLord = calculationSellResultRequest.getIsWWLandLord();
            }

            if(isWWLandLord){
                selectNo = 1;
            }else{
                selectNo = 2;
            }*/

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
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 013
         * 분기명 : 보유기간
         * 분기설명 : (1주택)양도주택의 보유기간
         */
        public CalculationSellResultResponse branchNo013(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo013 - 양도소득세 분기번호 013 : 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "013")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String buyDate = (house.getBuyDate() != null) ? house.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("buyDate : " + buyDate);
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, buyDate, sellDate, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 014
         * 분기명 : 보유기간
         * 분기설명 : (1주택)양도주택의 보유기간
         */
        public CalculationSellResultResponse branchNo014(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo014 - 양도소득세 분기번호 014 : 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "014")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String buyDate = (house.getBuyDate() != null) ? house.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("buyDate : " + buyDate);
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, buyDate, sellDate, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 015
         * 분기명 : 양도가액
         * 분기설명 : (1주택)양도가액 12억 초과 여부 확인
         */
        public CalculationSellResultResponse branchNo015(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo015 - 양도소득세 분기번호 015 : 양도가액");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "015")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String sellPrice = calculationSellResultRequest.getSellPrice().toString();
            log.info("sellPrice : " + sellPrice + "원");

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PRICE, sellPrice, EMPTY, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 016
         * 분기명 : 보유기간
         * 분기설명 : (1주택)양도주택의 보유기간
         */
        public CalculationSellResultResponse branchNo016(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo016 - 양도소득세 분기번호 016 : 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "016")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String buyDate = (house.getBuyDate() != null) ? house.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("buyDate : " + buyDate);
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, buyDate, sellDate, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 017
         * 분기명 : 거주기간
         * 분기설명 : (1주택)양도주택의  거주기간
         */
        public CalculationSellResultResponse branchNo017(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo017 - 양도소득세 분기번호 017 : 거주기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "017")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            long stayPeriodYear = 0;
            long stayPeriodMonth = 0;

            List<CalculationAdditionalAnswerRequest> additionalAnswerList = calculationSellResultRequest.getAdditionalAnswerList();
            for(CalculationAdditionalAnswerRequest answer : additionalAnswerList){
                if(PERIOD_TYPE_DIAL.equals(answer.getQuestionId()) || PERIOD_TYPE_CERT.equals(answer.getQuestionId())){
                    Map<String, Object> stayPeriodMap = getStayPeriodYearAndMonth(answer.getAnswerValue());
                    if(stayPeriodMap.containsKey(STAY_PERIOD_YEAR)){
                        stayPeriodYear = (long)stayPeriodMap.get(STAY_PERIOD_YEAR);
                    }
                    if(stayPeriodMap.containsKey(STAY_PERIOD_MONTH)){
                        stayPeriodMonth = (long)stayPeriodMap.get(STAY_PERIOD_MONTH);
                    }
                    break;
                }
            }

            log.info("stayPeriodYear : " + stayPeriodYear + "년");
            log.info("stayPeriodMonth : " + stayPeriodMonth + "개월");

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, Long.toString(stayPeriodYear), Long.toString(stayPeriodMonth), variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 018
         * 분기명 : 보유기간
         * 분기설명 : (1주택)양도주택의 보유기간
         */
        public CalculationSellResultResponse branchNo018(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo018 - 양도소득세 분기번호 018 : 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "018")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String buyDate = (house.getBuyDate() != null) ? house.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("buyDate : " + buyDate);
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, buyDate, sellDate, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 019
         * 분기명 : 두 주택 취득일 동일 여부(2주택)
         * 분기설명 : (2주택)두 주택의 취득일이 같은지 여부
         */
        public CalculationSellResultResponse branchNo019(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo019 - 양도소득세 분기번호 019 : 두 주택 취득일 동일 여부(2주택)");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "019")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(houseUtil.countOwnHouse() != 2){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "계산오류가 발생했습니다.(2주택 오류)");
            }

            List<House> houseList = houseUtil.findOwnHouseList();

            House oldHouse = getOldOrNewHouse(houseList, false);    // 종전주택
            House newHouse = getOldOrNewHouse(houseList, true);     // 신규주택

            if(oldHouse.getBuyDate() != null && newHouse.getBuyDate() != null){
                // 두 주택의 취득일이 동일한 경우
                if(oldHouse.getBuyDate() == newHouse.getBuyDate()){
                    selectNo = 1;
                }
                // 두 주택의 취득일이 동일하지 않은 경우
                else{
                    selectNo = 2;
                }
            }else{
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "보유주택의 취득일 계산 중 오류가 발생했습니다.");
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
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 020
         * 분기명 : 매도 대상 주택 구분
         * 분기설명 : (2주택)매도 대상이 종전주택인지 신규주택인지 구분
         */
        public CalculationSellResultResponse branchNo020(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo020 - 양도소득세 분기번호 020 : 매도 대상 주택 구분");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "020")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(houseUtil.countOwnHouse() != 2){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "계산오류가 발생했습니다.(2주택 오류)");
            }

            House oldHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), false);    // 종전주택

            // 양도주택이 종전주택인 경우
            if(calculationSellResultRequest.getHouseId().equals(oldHouse.getHouseId())){
                selectNo = 1;
            }
            // 양도주택이 신규주택인 경우
            else{
                selectNo = 2;
            }

            // 신규주택여부
            boolean isNewHouse = false;

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
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 021
         * 분기명 : 종전주택 매도 시 신규주택의 구분
         * 분기설명 : (2주택)종전주택을 매도할 때 신규주택의 구분
         */
        public CalculationSellResultResponse branchNo021(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo021 - 양도소득세 분기번호 021 : 종전주택 매도 시 신규주택의 구분");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "021")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(houseUtil.countOwnHouse() != 2){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "계산오류가 발생했습니다.(2주택 오류)");
            }

            House newHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), true);     // 신규주택

            if(FIVE.equals(newHouse.getHouseType()) || THREE.equals(newHouse.getHouseType()) || newHouse.getIsMoveInRight()){
                selectNo = 2;   // 준공 분양권(선택번호:2)
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
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 022
         * 분기명 : 신규주택 취득 전 종전주택 보유기간
         * 분기설명 : (2주택)종전주택 취득일로부터 [1]년이 된 날 다음날 이후 신규주택 취득 여부
         */
        public CalculationSellResultResponse branchNo022(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo022 - 양도소득세 분기번호 022 : 신규주택 취득 전 종전주택 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "022")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(houseUtil.countOwnHouse() != 2){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "계산오류가 발생했습니다.(2주택 오류)");
            }

            House oldHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), false);    // 종전주택
            House newHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), true);     // 신규주택

            String oldHouseBuyDateStr = (oldHouse.getBuyDate() != null) ? oldHouse.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String newHouseBuyDateStr = (newHouse.getBuyDate() != null) ? newHouse.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("oldHouseBuyDateStr : " + oldHouseBuyDateStr);
            log.info("newHouseBuyDateStr : " + newHouseBuyDateStr);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, oldHouseBuyDateStr, newHouseBuyDateStr, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 023
         * 분기명 : 종전주택 보유기간
         * 분기설명 : (2주택)종전주택을 취득일로부터 [2]년이 된 날 이후 매도 여부
         */
        public CalculationSellResultResponse branchNo023(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo023 - 양도소득세 분기번호 023 : 종전주택 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "023")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(houseUtil.countOwnHouse() != 2){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "계산오류가 발생했습니다.(2주택 오류)");
            }

            House oldHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), false);    // 종전주택

            String oldHouseBuyDateStr = (oldHouse.getBuyDate() != null) ? oldHouse.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String oldHouseSellDateStr = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("oldHouseBuyDateStr : " + oldHouseBuyDateStr);
            log.info("oldHouseSellDateStr : " + oldHouseSellDateStr);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, oldHouseBuyDateStr, oldHouseSellDateStr, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 024
         * 분기명 : 신규주택 보유기간에 따른 종전주택 매도일자
         * 분기설명 : (2주택)신규주택 취득일로부터 [3]년이 된 날 다음날 이내에 종전주택 매도 (예정)여부
         */
        public CalculationSellResultResponse branchNo024(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo024 - 양도소득세 분기번호 024 : 신규주택 보유기간에 따른 종전주택 매도일자");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "024")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(houseUtil.countOwnHouse() != 2){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "계산오류가 발생했습니다.(2주택 오류)");
            }

            House newHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), true);    // 신규주택

            String newHouseBuyDateStr = (newHouse.getBuyDate() != null) ? newHouse.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String oldHouseSellDateStr = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("newHouseBuyDateStr : " + newHouseBuyDateStr);
            log.info("oldHouseSellDateStr : " + oldHouseSellDateStr);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, newHouseBuyDateStr, oldHouseSellDateStr, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 025
         * 분기명 : 신규주택 취득 전 종전주택 보유기간
         * 분기설명 : (2주택)종전주택 취득일로부터 [1]년이 된 날 다음날 이후 신규주택 취득 여부
         */
        public CalculationSellResultResponse branchNo025(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo025 - 양도소득세 분기번호 025 : 신규주택 취득 전 종전주택 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "025")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(houseUtil.countOwnHouse() != 2){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "계산오류가 발생했습니다.(2주택 오류)");
            }

            House oldHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), false);    // 종전주택
            House newHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), true);     // 신규주택

            String oldHouseBuyDateStr = (oldHouse.getBuyDate() != null) ? oldHouse.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String newHouseBuyDateStr = (newHouse.getBuyDate() != null) ? newHouse.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("oldHouseBuyDateStr : " + oldHouseBuyDateStr);
            log.info("newHouseBuyDateStr : " + newHouseBuyDateStr);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, oldHouseBuyDateStr, newHouseBuyDateStr, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 026
         * 분기명 : 종전주택 보유기간
         * 분기설명 : (2주택)종전주택을 취득일로부터 [2]년이 된 날 이후 매도 여부
         */
        public CalculationSellResultResponse branchNo026(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo026 - 양도소득세 분기번호 026 : 종전주택 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "026")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(houseUtil.countOwnHouse() != 2){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "계산오류가 발생했습니다.(2주택 오류)");
            }

            House oldHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), false);    // 종전주택

            String oldHouseBuyDateStr = (oldHouse.getBuyDate() != null) ? oldHouse.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String oldHouseSellDateStr = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("oldHouseBuyDateStr : " + oldHouseBuyDateStr);
            log.info("oldHouseSellDateStr : " + oldHouseSellDateStr);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, oldHouseBuyDateStr, oldHouseSellDateStr, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 027
         * 분기명 : 취득일자 확인
         * 분기설명 : (2주택)신규주택 취득일 [2022.02.15] 이전 여부
         */
        public CalculationSellResultResponse branchNo027(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo025 - 양도소득세 분기번호 027 : 신규주택 취득 전 종전주택 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "027")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(houseUtil.countOwnHouse() != 2){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "계산오류가 발생했습니다.(2주택 오류)");
            }

            House newHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), true);    // 신규주택

            String newHouseBuyDateStr = (newHouse.getBuyDate() != null) ? newHouse.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("newHouseBuyDateStr : " + newHouseBuyDateStr);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_DATE, newHouseBuyDateStr, EMPTY, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 028
         * 분기명 : 종전주택 보유기간
         * 분기설명 : (2주택)종전주택을 취득일로부터 [2]년이 된 날 이후 매도 (예정)여부
         */
        public CalculationSellResultResponse branchNo028(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo028 - 양도소득세 분기번호 028 : 종전주택 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "028")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(houseUtil.countOwnHouse() != 2){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "계산오류가 발생했습니다.(2주택 오류)");
            }

            House oldHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), false);    // 종전주택

            String oldHouseBuyDateStr = (oldHouse.getBuyDate() != null) ? oldHouse.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String oldHouseSellDateStr = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("oldHouseBuyDateStr : " + oldHouseBuyDateStr);
            log.info("oldHouseSellDateStr : " + oldHouseSellDateStr);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, oldHouseBuyDateStr, oldHouseSellDateStr, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 029
         * 분기명 : 신규주택 보유기간에 따른 종전주택 매도일자
         * 분기설명 : (2주택)신규주택 취득일로부터 [3]년이 된 날 다음날 이내에 종전주택 매도 (예정)여부
         */
        public CalculationSellResultResponse branchNo029(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo029 - 양도소득세 분기번호 029 : 신규주택 보유기간에 따른 종전주택 매도일자");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "029")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(houseUtil.countOwnHouse() != 2){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "계산오류가 발생했습니다.(2주택 오류)");
            }

            House newHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), true);    // 신규주택

            String newHouseBuyDateStr = (newHouse.getBuyDate() != null) ? newHouse.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String oldHouseSellDateStr = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("newHouseBuyDateStr : " + newHouseBuyDateStr);
            log.info("oldHouseSellDateStr : " + oldHouseSellDateStr);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, newHouseBuyDateStr, oldHouseSellDateStr, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 030
         * 분기명 : 신규주택 거주 계획
         * 분기설명 : (2주택)신규주택에 주민등록초본상 전입일로부터 1년이 된날 이후까지 계속 거주 계획 여부(사용자 선택)
         */
        public CalculationSellResultResponse branchNo030(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo030 - 양도소득세 분기번호 030 : 신규주택 거주 계획");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "030")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            List<CalculationAdditionalAnswerRequest> additionalAnswerList = calculationSellResultRequest.getAdditionalAnswerList();
            for(CalculationAdditionalAnswerRequest answer : additionalAnswerList){
                if(Q_0001.equals(answer.getQuestionId())){
                    if(ANSWER_VALUE_01.equals(answer.getAnswerValue())){
                        selectNo = 1;
                    }else{
                        selectNo = 2;
                    }
                }
            }

            if(selectNo == 0){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "최근 취득한 주택에 1년이상 거주 예정 여부에 대한 추가 질의 값을 받지 못했습니다.");
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
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 031
         * 분기명 : 양도일이 중과정책적용 기간인지 여부
         * 분기설명 : (2주택)양도일이 중과정책적용 기간에 해당하는지 ([2025.05.10] 이후) 여부
         */
        public CalculationSellResultResponse branchNo031(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo031 - 양도소득세 분기번호 031 : 매도일이 중과정책적용 기간인지 여부");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "031")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_DATE, sellDate, EMPTY, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 032
         * 분기명 : 조정대상지역여부
         * 분기설명 : (2주택)양도일 기준 조정대상지역에 해당하는지 여부
         */
        public CalculationSellResultResponse branchNo032(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo010 - 양도소득세 분기번호 032 : 조정대상지역여부");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "032")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 조정대상지역여부(TODO. 양도일 기준으로 조정대상지역 체크하도록 수정 필요)
            //boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(house.getRoadAddr()));
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(house.getJibunAddr()), calculationSellResultRequest.getSellDate());

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
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /*
         * 분기번호 : 033 ~ 036 SKIP
         * TODO.중과정책적용 시점에 맞춰 구현 예정
         */

        /**
         * 분기번호 : 037
         * 분기명 : 보유기간
         * 분기설명 : (2주택)양도주택의 보유기간
         */
        public CalculationSellResultResponse branchNo037(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo037 - 양도소득세 분기번호 037 : 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "037")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String buyDate = (house.getBuyDate() != null) ? house.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("buyDate : " + buyDate);
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, buyDate, sellDate, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 038
         * 분기명 : 보유기간
         * 분기설명 : (2주택)양도주택 취득일로부터 [3]년이 된날 이후 여부(장기보유특별공제 대상여부)
         */
        public CalculationSellResultResponse branchNo038(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo038 - 양도소득세 분기번호 038 : 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "038")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String buyDate = (house.getBuyDate() != null) ? house.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("buyDate : " + buyDate);
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, buyDate, sellDate, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 039
         * 분기명 : 양도일이 중과정책적용 기간인지 여부
         * 분기설명 : (3주택)양도일이 중과정책적용 기간에 해당하는지 ([2025.05.10] 이후) 여부
         */
        public CalculationSellResultResponse branchNo039(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo039 - 양도소득세 분기번호 039 : 매도일이 중과정책적용 기간인지 여부");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "039")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_DATE, sellDate, EMPTY, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /*
         * 분기번호 : 040 ~ 043 SKIP
         * TODO.중과정책적용 시점에 맞춰 구현 예정
         */

        /**
         * 분기번호 : 044
         * 분기명 : 보유기간
         * 분기설명 : (3주택)양도주택의 보유기간
         */
        public CalculationSellResultResponse branchNo044(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo044 - 양도소득세 분기번호 044 : 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "044")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String buyDate = (house.getBuyDate() != null) ? house.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("buyDate : " + buyDate);
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, buyDate, sellDate, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 045
         * 분기명 : 보유기간
         * 분기설명 : (3주택)양도주택 취득일로부터 [3]년이 된날 이후 여부(장기보유특별공제 대상여부)
         */
        public CalculationSellResultResponse branchNo045(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo045 - 양도소득세 분기번호 045 : 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "045")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String buyDate = (house.getBuyDate() != null) ? house.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("buyDate : " + buyDate);
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, buyDate, sellDate, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 046
         * 분기명 : 주택보유여부
         * 분기설명 : (1주택)양도주택의 취득 계약일 당시 주택 보유여부
         */
        public CalculationSellResultResponse branchNo046(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo046 - 양도소득세 분기번호 046 : 주택보유여부");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "046")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            List<CalculationAdditionalAnswerRequest> additionalAnswerList = calculationSellResultRequest.getAdditionalAnswerList();
            for(CalculationAdditionalAnswerRequest answer : additionalAnswerList){
                if(Q_0008.equals(answer.getQuestionId())){
                    if(ANSWER_VALUE_01.equals(answer.getAnswerValue())){
                        selectNo = 1;
                    }else{
                        selectNo = 2;
                    }
                }
            }

            if(selectNo == 0){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도주택의 취득 계약일 당시 주택 보유여부에 대한 추가 질의 값을 받지 못했습니다.");
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
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 047
         * 분기명 : 조정대상지역여부
         * 분기설명 : (1주택)계약일 기준 조정대상지역 기간해당 여부
         */
        public CalculationSellResultResponse branchNo047(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo047 - 양도소득세 분기번호 047 : 조정대상지역여부");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "047")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            // 조정대상지역여부(TODO. 계약일 기준으로 조정대상지역 체크하도록 수정)
            //boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(house.getRoadAddr()));
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(house.getJibunAddr()), house.getContractDate());

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
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 048
         * 분기명 : 신규주택 보유기간에 따른 종전주택 매도일자
         * 분기설명 : (2주택)신규주택 취득일로부터 [3]년이 된 날 다음날 이내에 종전주택 매도 (예정)여부
         */
        public CalculationSellResultResponse branchNo048(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo048 - 양도소득세 분기번호 048 : 신규주택 보유기간에 따른 종전주택 매도일자");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "048")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            if(houseUtil.countOwnHouse() != 2){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "계산오류가 발생했습니다.(2주택 오류)");
            }

            House newHouse = getOldOrNewHouse(houseUtil.findOwnHouseList(), true);    // 신규주택

            String newHouseBuyDateStr = (newHouse.getBuyDate() != null) ? newHouse.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String oldHouseSellDateStr = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("newHouseBuyDateStr : " + newHouseBuyDateStr);
            log.info("oldHouseSellDateStr : " + oldHouseSellDateStr);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, newHouseBuyDateStr, oldHouseSellDateStr, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 049
         * 분기명 : 양도가액
         * 분기설명 : (1주택)양도가액 12억 초과 여부 확인
         */
        public CalculationSellResultResponse branchNo049(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo049 - 양도소득세 분기번호 049 : 양도가액");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "049")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String sellPrice = calculationSellResultRequest.getSellPrice().toString();
            log.info("sellPrice : " + sellPrice + "원");

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PRICE, sellPrice, EMPTY, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 050
         * 분기명 : 보유기간
         * 분기설명 : (1주택)양도주택의 보유기간
         */
        public CalculationSellResultResponse branchNo050(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo050 - 양도소득세 분기번호 050 : 보유기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "050")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            String buyDate = (house.getBuyDate() != null) ? house.getBuyDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String sellDate = (calculationSellResultRequest.getSellDate() != null) ? calculationSellResultRequest.getSellDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("buyDate : " + buyDate);
            log.info("sellDate : " + sellDate);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, buyDate, sellDate, variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 051
         * 분기명 : 거주기간
         * 분기설명 : (1주택)양도주택의  거주기간
         */
        public CalculationSellResultResponse branchNo051(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo051 - 양도소득세 분기번호 051 : 거주기간");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "051")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            long stayPeriodYear = 0;
            long stayPeriodMonth = 0;

            List<CalculationAdditionalAnswerRequest> additionalAnswerList = calculationSellResultRequest.getAdditionalAnswerList();
            for(CalculationAdditionalAnswerRequest answer : additionalAnswerList){
                if(PERIOD_TYPE_DIAL.equals(answer.getQuestionId()) || PERIOD_TYPE_CERT.equals(answer.getQuestionId())){
                    Map<String, Object> stayPeriodMap = getStayPeriodYearAndMonth(answer.getAnswerValue());
                    if(stayPeriodMap.containsKey(STAY_PERIOD_YEAR)){
                        stayPeriodYear = (long)stayPeriodMap.get(STAY_PERIOD_YEAR);
                    }
                    if(stayPeriodMap.containsKey(STAY_PERIOD_MONTH)){
                        stayPeriodMonth = (long)stayPeriodMap.get(STAY_PERIOD_MONTH);
                    }
                    break;
                }
            }

            log.info("stayPeriodYear : " + stayPeriodYear + "년");
            log.info("stayPeriodMonth : " + stayPeriodMonth + "개월");

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, Long.toString(stayPeriodYear), Long.toString(stayPeriodMonth), variableData, dataMethod)){
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

            if(hasNext){
                try{
                    Method method = calculationBranchClass.getMethod("branchNo" + nextBranchNo, CalculationSellResultRequest.class, House.class);
                    calculationSellResultResponse = (CalculationSellResultResponse) method.invoke(target, calculationSellResultRequest, house);
                }catch(Exception e){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED);
                }
            }else{
                calculationSellResultResponse = getCalculationSellResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /*============================================================ 양도소득세 계산 프로세스 END ============================================================*/

        // 양도소득세 계산 결과 조회
        private CalculationSellResultResponse getCalculationSellResultResponse(CalculationSellResultRequest calculationSellResultRequest, String taxRateCode, String dedCode){
            log.info(">>> CalculationBranch getCalculationSellResult - 양도소득세 계산 수행");

            // 양도소득세 계산 결과 세팅
            List<CalculationSellOneResult> calculationSellResultOneList = new ArrayList<>();

            // 양도주택정보
            House house = houseRepository.findByHouseId(calculationSellResultRequest.getHouseId())
                    .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR));

            // (양도주택)조정지역여부
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(house.getJibunAddr()), house.getBuyDate());

            // 세율정보
            TaxRateInfo taxRateInfo = null;
            if(taxRateCode != null && !taxRateCode.isBlank()){
                log.info("세율정보 조회 : " + taxRateCode);
                taxRateInfo = taxRateInfoRepository.findByTaxRateCode(taxRateCode)
                        .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "세율 정보를 가져오는 중 오류가 발생했습니다."));
            }

            // 공제정보
            DeductionInfo deductionInfo = null;
            if(dedCode != null && !dedCode.isBlank()){
                log.info("공제정보 조회 : " + dedCode);
                deductionInfo = deductionInfoRepository.findByDedCode(dedCode)
                        .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "공제 정보를 가져오는 중 오류가 발생했습니다."));
            }

            // 비과세 여부
            boolean isNonTaxRate = false;

            int ownerCount = calculationSellResultRequest.getOwnerCnt();            // (양도주택)소유자 수
            int userProportionPercentage = calculationSellResultRequest.getUserProportion();

            // 소유자수와 보유주택비율이 보유주택의 DB정보와 사용자의 입력값이 다른 경우, 사용자의 입력값으로 DB 업데이트 처리
            if(ownerCount != house.getOwnerCnt() || userProportionPercentage != house.getUserProportion()){
                house.setOwnerCnt(ownerCount);
                house.setUserProportion(userProportionPercentage);

                houseRepository.saveAndFlush(house);
            }

            double userProportion = (double)userProportionPercentage / 100;         // (양도주택)보유주택비율(소유자1)
            double restProPortion = 1 - userProportion;                             // (양도주택)보유주택비율(소유자2)

            log.info("- 보유주택 수 : " + houseUtil.countOwnHouse());

            for(int i=0; i<ownerCount; i++) {
                double proportion = 1;
                if (ownerCount > 1) {
                    if (i == 0) proportion = userProportion;
                    else proportion = restProPortion;
                }

                log.info("- 보유주택비율 : " + proportion);

                long sellProfitPrice = 0;       // 양도차익금액
                long taxableStdPrice = 0;       // 과세표준금액
                long sellTaxPrice = 0;          // 양도소득세액
                long localTaxPrice = 0;         // 지방소득세액
                long nonTaxablePrice = 0;       // 비과세대상양도차익금액
                long taxablePrice = 0;          // 과세대상양도차익금액
                long longDeductionPrice = 0;    // 장기보유특별공제금액
                long sellIncomePrice = 0;       // 양도소득금액
                long basicDeductionPrice = 0;   // 기본공제금액
                long progDeductionPrice = 0;    // 누진공제금액
                long totalTaxPrice = 0;         // 총납부세액
                long retentionPeriodDay = 0;    // 보유기간(일)
                long retentionPeriodYear = 0;   // 보유기간(년)

                double sellTaxRate = 0;         // 양도소득세율
                double localTaxRate = 0;        // 지방소득세율
                double taxRate1 = 0;            // 세율1
                double taxRate2 = 0;            // 세율2
                double addTaxRate1 = 0;         // 추가세율1
                double addTaxRate2 = 0;         // 추가세율2
                double finalTaxRate1 = 0;       // 최종세율1
                double finalTaxRate2 = 0;       // 최종세율2

                double dedRate = 0;             // 공제율

                long buyPrice = (long)(house.getBuyPrice() * proportion);                       // 취득가액(지분율 적용)
                LocalDate buyDate = house.getBuyDate();                                         // 취득일자
                long totalSellPrice = calculationSellResultRequest.getSellPrice();              // 전체 양도가액
                long sellPrice = (long)(totalSellPrice * proportion);                           // 양도가액(지분율 적용)
                LocalDate sellDate = calculationSellResultRequest.getSellDate();                // 양도일자
                long necExpensePrice = calculationSellResultRequest.getNecExpensePrice();       // 필요경비금액
                necExpensePrice = (long)(necExpensePrice * proportion);                         // 필요경비금액(지분율 적용)
                sellProfitPrice = sellPrice - (buyPrice + necExpensePrice);                     // 양도차익금액(양도가액 - (취득가액 + 필요경비))

                // 양도차익금액이 0보다 작으면 0으로 세팅
                if(sellProfitPrice < 0) sellProfitPrice = 0;

                retentionPeriodDay = ChronoUnit.DAYS.between(buyDate, sellDate);                // 보유기간(일)
                retentionPeriodYear = ChronoUnit.YEARS.between(buyDate, sellDate);              // 보유기간(년)

                log.info("----------------------------------");
                log.info("- 취득가액 : " + buyPrice);
                log.info("- 취득일자 : " + buyDate);
                log.info("- 양도가액 : " + sellPrice);
                log.info("- 양도일자 : " + sellDate);
                log.info("- 필요경비금액 : " + necExpensePrice);
                log.info("- 양도차익금액 : " + sellProfitPrice);
                log.info("- 보유기간(일) : " + retentionPeriodDay);
                log.info("- 보유기간(년) : " + retentionPeriodYear);
                log.info("----------------------------------");

                /* 양도소득세 계산 */
                log.info("양도소득세 계산 START");
                if(taxRateInfo != null){
                    // 기본공제금액
                    basicDeductionPrice = BASIC_DEDUCTION_PRICE;

                    // 세율이 상수인 경우
                    if(YES.equals(taxRateInfo.getConstYn())){
                        if(sellProfitPrice != 0){
                            // 과세대상양도차익금액
                            taxablePrice = sellProfitPrice;

                            // 양도소득금액(과세대상양도차익금액)
                            sellIncomePrice = sellProfitPrice;

                            // 과세표준금액(양도소득금액 - 기본공제금액)
                            taxableStdPrice = sellIncomePrice - basicDeductionPrice;
                            if(taxableStdPrice < 0) taxableStdPrice = 0;

                            // 양도소득세율
                            sellTaxRate = Double.parseDouble(StringUtils.defaultString(taxRateInfo.getTaxRate1(), ZERO));

                            // 양도소득세액
                            sellTaxPrice = (long)(taxableStdPrice * sellTaxRate) - progDeductionPrice;
                            
                            // 양도소득세액이 0보다 작거나 같은 경우, 양도소득세율과 양도소득세액 모두 0으로 세팅
                            if(sellTaxPrice <= 0){
                                sellTaxRate = 0;
                                sellTaxPrice = 0;
                            }
                        }
                    }
                    // 세율이 상수가 아닌 경우(변수)
                    else{
                        if(sellProfitPrice != 0){
                            // 세율1이 비과세인지 체크(비과세대상양도차익금액 세팅여부를 확인)
                            if(NONE_AND_GENERAL_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                                // 기준금액(12억)
                                if(taxRateInfo.getBasePrice() != null){
                                    // 과세대상양도차익 = 양도차익 x (전체양도가액 - 12억) / 전체양도가액
                                    taxablePrice = sellProfitPrice * (totalSellPrice - taxRateInfo.getBasePrice()) / totalSellPrice;
                                }

                                // 비과세대상양도차익금액 = 양도차익 - 과세대상양도차익
                                nonTaxablePrice = sellProfitPrice - taxablePrice;
                            }else{
                                taxablePrice = sellProfitPrice;
                            }

                            // 과세대상양도차익금액
                            //taxablePrice = sellProfitPrice - nonTaxablePrice;

                            // 양도차익금액이 0보다 작으면 0으로 세팅
                            if(taxablePrice < 0) taxablePrice = 0;

                            // 공제율 및 장기보유특별공제금액(공제정보가 존재하는 경우에만 계산)
                            if(deductionInfo != null){
                                long stayPeriodYear = 0;

                                List<CalculationAdditionalAnswerRequest> additionalAnswerList = calculationSellResultRequest.getAdditionalAnswerList();
                                for(CalculationAdditionalAnswerRequest answer : additionalAnswerList){
                                    if(PERIOD_TYPE_DIAL.equals(answer.getQuestionId()) || PERIOD_TYPE_CERT.equals(answer.getQuestionId())){
                                        Map<String, Object> stayPeriodMap = getStayPeriodYearAndMonth(answer.getAnswerValue());
                                        if(stayPeriodMap.containsKey(STAY_PERIOD_YEAR)){
                                            stayPeriodYear = (long)stayPeriodMap.get(STAY_PERIOD_YEAR);
                                        }
                                        break;
                                    }
                                }

                                // 공제율
                                dedRate = calculateDeductionRate(deductionInfo, retentionPeriodYear, stayPeriodYear);

                                // 장기보유특별공제금액(과세대상양도차익금액 x 공제율)
                                longDeductionPrice = (long)(taxablePrice * dedRate);
                            }

                            // 양도소득금액(과세대상양도차익금액 - 장기보유특별공제금액)
                            sellIncomePrice = taxablePrice - longDeductionPrice;
                            if(sellIncomePrice < 0) sellIncomePrice = 0;

                            // 과세표준금액(양도소득금액 - 기본공제금액)
                            taxableStdPrice = sellIncomePrice - basicDeductionPrice;
                            if(taxableStdPrice < 0) taxableStdPrice = 0;

                            // 누진공제금액
                            progDeductionPrice = calculateProgDeductionPrice(taxableStdPrice);

                            if(taxRateInfo.getTaxRate1() != null && !taxRateInfo.getTaxRate1().isBlank()){
                                // 세율이 2개인 경우
                                if(taxRateInfo.getTaxRate2() != null && !taxRateInfo.getTaxRate2().isBlank()){
                                    // 세율1
                                    if(GENERAL_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                                        taxRate1 = calculateGeneralTaxRate(taxableStdPrice);
                                    }else if(NONE_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                                        taxRate1 = 0;
                                    }else if(NONE_AND_GENERAL_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                                        taxRate1 = calculateGeneralTaxRate(taxableStdPrice);
                                    }else{
                                        taxRate1 = Double.parseDouble(StringUtils.defaultString(taxRateInfo.getTaxRate1(), ZERO));
                                    }

                                    // 세율2
                                    if(GENERAL_TAX_RATE.equals(taxRateInfo.getTaxRate2())){
                                        taxRate2 = calculateGeneralTaxRate(taxableStdPrice);
                                    }else if(NONE_TAX_RATE.equals(taxRateInfo.getTaxRate2())){
                                        taxRate2 = 0;
                                    }else if(NONE_AND_GENERAL_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                                        taxRate2 = calculateGeneralTaxRate(taxableStdPrice);
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

                                    finalTaxRate1 = taxRate1 + addTaxRate1;     // 최종세율1
                                    finalTaxRate2 = taxRate2 + addTaxRate2;     // 최종세율2

                                    // 사용함수
                                    String usedFunc = StringUtils.defaultString(taxRateInfo.getUsedFunc());

                                    // MAX : 세율1과 세율2 중 최대값 사용
                                    if(MAX.equals(usedFunc)){
                                        // 양도소득세율
                                        sellTaxRate = Math.max(finalTaxRate1, finalTaxRate2);

                                        // 양도소득세액((과세표준 x 양도소득세율) - 누진공제금액)
                                        sellTaxPrice = (long)(taxableStdPrice * sellTaxRate) - progDeductionPrice;

                                        // 양도소득세액이 0보다 작은 경우, 양도소득세율과 양도소득세액 모두 0으로 세팅
                                        if(sellTaxPrice < 0){
                                            sellTaxRate = 0;
                                            sellTaxPrice = 0;
                                        }
                                    }
                                }
                                // 세율이 1개인 경우
                                else{
                                    if(GENERAL_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                                        taxRate1 = calculateGeneralTaxRate(taxableStdPrice);
                                    }else if(NONE_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                                        taxRate1 = 0;
                                    }else if(NONE_AND_GENERAL_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                                        taxRate1 = calculateGeneralTaxRate(taxableStdPrice);
                                    }else{
                                        taxRate1 = Double.parseDouble(StringUtils.defaultString(taxRateInfo.getTaxRate1(), ZERO));
                                    }

                                    // 추가세율
                                    if(taxRateInfo.getAddTaxRate1() != null && !taxRateInfo.getAddTaxRate1().isBlank()){
                                        addTaxRate1 = Double.parseDouble(StringUtils.defaultString(taxRateInfo.getAddTaxRate1(), ZERO));
                                    }

                                    // 최종세율1
                                    finalTaxRate1 = taxRate1 + addTaxRate1;

                                    // 양도소득세율
                                    sellTaxRate = finalTaxRate1;

                                    // 양도소득세액((과세표준 x 양도소득세율) - 누진공제금액)
                                    sellTaxPrice = (long)(taxableStdPrice * sellTaxRate) - progDeductionPrice;

                                    // 양도소득세액이 0보다 작은 경우, 양도소득세율과 양도소득세액 모두 0으로 세팅
                                    if(sellTaxPrice < 0){
                                        sellTaxRate = 0;
                                        sellTaxPrice = 0;
                                    }
                                }
                            }
                        }
                    }

                    // 지방소득세율(양도소득세율의 10%)
                    localTaxRate = sellTaxRate * 0.1;

                    // 지방소득세액(양도소득세액의 10%)
                    localTaxPrice = (long)(sellTaxPrice * 0.1);

                    // 총납부세액(양도소득세 + 지방소득세)
                    totalTaxPrice = sellTaxPrice + localTaxPrice;
                }

                log.info("양도소득세 계산 END");

                log.info("----------------------------------");
                log.info("- 비과세대상양도차익금액 : " + nonTaxablePrice);
                log.info("- 과세대상양도차익금액 : " + taxablePrice);
                log.info("- 장기보유특별공제금액 : " + longDeductionPrice);
                log.info("- 양도소득금액 : " + sellIncomePrice);
                log.info("- 기본공제금액 : " + basicDeductionPrice);
                log.info("- 과세표준금액 : " + taxableStdPrice);
                log.info("- 양도소득세율 : " + sellTaxRate);
                log.info("- 지방소득세율 : " + localTaxRate);
                log.info("- 누진공제금액 : " + progDeductionPrice);
                log.info("- 양도소득세액 : " + sellTaxPrice);
                log.info("- 지방소득세액 : " + localTaxPrice);
                log.info("- 총납부세액 : " + totalTaxPrice);
                log.info("----------------------------------");

                String proportionRateStr = String.format("%.0f", proportion*100);

                String buyPriceStr = Long.toString(buyPrice);
                String buyDateStr = buyDate.toString();
                String sellPriceStr = Long.toString(sellPrice);
                String sellDateStr = sellDate.toString();
                String necExpensePriceStr = Long.toString(necExpensePrice);
                String sellProfitPriceStr = Long.toString(sellProfitPrice);
                String retentionPeriodStr = Long.toString(retentionPeriodYear) + "년 이상";

                String nonTaxablePriceStr = Long.toString(nonTaxablePrice);
                String taxablePriceStr = Long.toString(taxablePrice);
                String longDeductionPriceStr = Long.toString(longDeductionPrice);
                String sellIncomePriceStr = Long.toString(sellIncomePrice);
                String basicDeductionPriceStr = Long.toString(basicDeductionPrice);

                String taxableStdPriceStr = Long.toString(taxableStdPrice);
                String sellTaxRateStr = String.format("%.0f", sellTaxRate*100);
                String localTaxRateStr = String.format("%.1f", localTaxRate*100);
                String progDeductionPriceStr = Long.toString(progDeductionPrice);
                String sellTaxPriceStr = Long.toString(sellTaxPrice);
                String localTaxPriceStr = Long.toString(localTaxPrice);

                String totalTaxPriceStr = Long.toString(totalTaxPrice);

                calculationSellResultOneList.add(
                        CalculationSellOneResult.builder()
                                .userProportion(proportionRateStr)
                                .buyPrice(buyPriceStr)
                                .buyDate(buyDateStr)
                                .sellPrice(sellPriceStr)
                                .sellDate(sellDateStr)
                                .necExpensePrice(necExpensePriceStr)
                                .sellProfitPrice(sellProfitPriceStr)
                                .retentionPeriod(retentionPeriodStr)
                                .nonTaxablePrice(nonTaxablePriceStr)
                                .taxablePrice(taxablePriceStr)
                                .longDeductionPrice(longDeductionPriceStr)
                                .sellIncomePrice(sellIncomePriceStr)
                                .basicDeductionPrice(basicDeductionPriceStr)
                                .taxableStdPrice(taxableStdPriceStr)
                                .sellTaxRate(sellTaxRateStr)
                                .localTaxRate(localTaxRateStr)
                                .progDeductionPrice(progDeductionPriceStr)
                                .sellTaxPrice(sellTaxPriceStr)
                                .localTaxPrice(localTaxPriceStr)
                                .totalTaxPrice(totalTaxPriceStr)
                                .build());
            }

            // 양도소득세 해설부분 세팅
            List<String> commentaryList = getCalculationSellCommentaryList(calculationSellResultRequest, taxRateCode, dedCode, calculationSellResultOneList);
            int commentaryListCnt = commentaryList.size();
            
            // 계산결과 텍스트 데이터 세팅
            String calculationResultTextData = getCalculationResultTextData(calculationSellResultRequest, calculationSellResultOneList, commentaryList);

            CalculationSellResultResponse calculationSellResultResponse = CalculationSellResultResponse.builder()
                    .listCnt(ownerCount)
                    .list(calculationSellResultOneList)
                    .commentaryListCnt(commentaryListCnt)
                    .commentaryList(commentaryList)
                    .calculationResultTextData(calculationResultTextData)
                    .build();

            // 양도소득세 계산 결과 이력 저장
            calculationSellResultResponse.setCalcHistoryId(saveCalculationSellHistory(calculationSellResultRequest, calculationSellResultResponse, isAdjustmentTargetArea));

            return calculationSellResultResponse;
        }

        // 양도소득세 계산 결과 이력 저장
        private Long saveCalculationSellHistory(CalculationSellResultRequest calculationSellResultRequest,
                                                CalculationSellResultResponse calculationSellResultResponse,
                                                boolean isAdjustmentTargetArea){
            log.info(">>> CalculationBranch saveCalculationSellHistory - 양도소득세 계산 결과 이력 저장");

            // 계산이력ID
            Long calcHistoryId = null;

            try{
                // 계산이력 저장 후 계산이력ID 추출
                calcHistoryId = calculationHistoryRepository.saveAndFlush(
                        CalculationHistory.builder()
                                .userId(userUtil.findCurrentUserId())
                                .calcType(CALC_TYPE_SELL)
                                .build()).getCalcHistoryId();

                if(calcHistoryId != null){
                    log.info("계산이력 저장 성공 > 계산이력ID : " + calcHistoryId);

                    // 계산양도소득세요청이력 저장
                    calculationSellRequestHistoryRepository.saveAndFlush(
                            CalculationSellRequestHistory.builder()
                                    .calcHistoryId(calcHistoryId)
                                    .sellHouseId(calculationSellResultRequest.getHouseId())
                                    .sellContractDate(calculationSellResultRequest.getSellContractDate())
                                    .sellDate(calculationSellResultRequest.getSellDate())
                                    .sellPrice(calculationSellResultRequest.getSellPrice())
                                    .necExpensePrice(calculationSellResultRequest.getNecExpensePrice())
                                    .build());

                    // 계산추가답변요청이력 저장
                    List<CalculationAdditionalAnswerRequest> calculationAdditionalAnswerRequestList = calculationSellResultRequest.getAdditionalAnswerList();
                    int calculationAdditionalAnswerRequestHistorySeq = 1;
                    for(CalculationAdditionalAnswerRequest calculationAdditionalAnswerRequest : calculationAdditionalAnswerRequestList){
                        calculationAdditionalAnswerRequestHistoryRepository.saveAndFlush(
                                CalculationAdditionalAnswerRequestHistory.builder()
                                        .calculationHistoryId(
                                                CalculationHistoryId.builder()
                                                        .calcHistoryId(calcHistoryId)
                                                        .detailHistorySeq(calculationAdditionalAnswerRequestHistorySeq)
                                                        .build())
                                        .questionId(calculationAdditionalAnswerRequest.getQuestionId())
                                        .answerValue(calculationAdditionalAnswerRequest.getAnswerValue())
                                        .build());
                        calculationAdditionalAnswerRequestHistorySeq++;
                    }

                    // 계산양도소득세응답이력 저장
                    List<CalculationSellOneResult> calculationSellOneResultList = calculationSellResultResponse.getList();
                    int calculationSellResponseHistorySeq = 1;
                    for(CalculationSellOneResult calculationSellOneResult : calculationSellOneResultList){
                        calculationSellResponseHistoryRepository.saveAndFlush(
                                CalculationSellResponseHistory.builder()
                                        .calculationHistoryId(
                                                CalculationHistoryId.builder()
                                                        .calcHistoryId(calcHistoryId)
                                                        .detailHistorySeq(calculationSellResponseHistorySeq)
                                                        .build())
                                        .userProportion(calculationSellOneResult.getUserProportion())
                                        .buyPrice(calculationSellOneResult.getBuyPrice())
                                        .buyDate(calculationSellOneResult.getBuyDate())
                                        .sellPrice(calculationSellOneResult.getSellPrice())
                                        .sellDate(calculationSellOneResult.getSellDate())
                                        .necExpensePrice(calculationSellOneResult.getNecExpensePrice())
                                        .sellProfitPrice(calculationSellOneResult.getSellProfitPrice())
                                        .retentionPeriod(calculationSellOneResult.getRetentionPeriod())
                                        .taxablePrice(calculationSellOneResult.getTaxablePrice())
                                        .nonTaxablePrice(calculationSellOneResult.getNonTaxablePrice())
                                        .longDeductionPrice(calculationSellOneResult.getLongDeductionPrice())
                                        .sellIncomePrice(calculationSellOneResult.getSellIncomePrice())
                                        .basicDeductionPrice(calculationSellOneResult.getBasicDeductionPrice())
                                        .taxableStdPrice(calculationSellOneResult.getTaxableStdPrice())
                                        .sellTaxRate(calculationSellOneResult.getSellTaxRate())
                                        .localTaxRate(calculationSellOneResult.getLocalTaxRate())
                                        .progDeductionPrice(calculationSellOneResult.getProgDeductionPrice())
                                        .sellTaxPrice(calculationSellOneResult.getSellTaxPrice())
                                        .localTaxPrice(calculationSellOneResult.getLocalTaxPrice())
                                        .totalTaxPrice(calculationSellOneResult.getTotalTaxPrice())
                                        .isAdjustmentTargetArea(isAdjustmentTargetArea)
                                        .build());
                        calculationSellResponseHistorySeq++;
                    }

                    // 계산해설응답이력 저장
                    List<String> commentaryList = calculationSellResultResponse.getCommentaryList();
                    int calculationCommentaryResponseHistorySeq = 1;
                    for(String commentary : commentaryList){
                        calculationCommentaryResponseHistoryRepository.saveAndFlush(
                                CalculationCommentaryResponseHistory.builder()
                                        .calculationHistoryId(
                                                CalculationHistoryId.builder()
                                                        .calcHistoryId(calcHistoryId)
                                                        .detailHistorySeq(calculationCommentaryResponseHistorySeq)
                                                        .build())
                                        .commentaryContent(commentary)
                                        .build()
                        );
                        calculationCommentaryResponseHistorySeq++;
                    }

                    // 계산보유주택이력 저장
                    Long ownHouseHistoryId = calculationOwnHouseHistoryRepository.saveAndFlush(
                            CalculationOwnHouseHistory.builder()
                                    .calcHistoryId(calcHistoryId)
                                    .ownHouseCnt(houseUtil.countOwnHouse())
                                    .hasOwnHouseDetail(houseUtil.countOwnHouse() > 0)
                                    .build()
                    ).getOwnHouseHistoryId();

                    // 계산보유주택이력상세 저장
                    List<House> houseList = houseUtil.findOwnHouseList();
                    int calculationOwnHouseHistoryDetail = 1;
                    for(House house : houseList){
                        calculationOwnHouseHistoryDetailRepository.saveAndFlush(
                                CalculationOwnHouseHistoryDetail.builder()
                                        .calculationOwnHouseHistoryId(
                                                CalculationOwnHouseHistoryId.builder()
                                                        .ownHouseHistoryId(ownHouseHistoryId)
                                                        .detailHistorySeq(calculationOwnHouseHistoryDetail)
                                                        .build())
                                        .houseId(house.getHouseId())
                                        .houseType(house.getHouseType())
                                        .houseName(house.getHouseName())
                                        .detailAdr(house.getDetailAdr())
                                        .contractDate(house.getContractDate())
                                        .balanceDate(house.getBalanceDate())
                                        .buyDate(house.getBuyDate())
                                        .buyPrice(house.getBuyPrice())
                                        .pubLandPrice(house.getPubLandPrice())
                                        .area(house.getArea())
                                        .kbMktPrice(house.getKbMktPrice())
                                        .jibunAddr(house.getJibunAddr())
                                        .roadAddr(house.getRoadAddr())
                                        .roadAddrRef(house.getRoadAddrRef())
                                        .bdMgtSn(house.getBdMgtSn())
                                        .admCd(house.getAdmCd())
                                        .rnMgtSn(house.getRnMgtSn())
                                        .isDestruction(house.getIsDestruction())
                                        .ownerCnt(house.getOwnerCnt())
                                        .userProportion(house.getUserProportion())
                                        .isMoveInRight(house.getIsMoveInRight())
                                        .sourceType(house.getSourceType())
                                        .build()
                        );
                        calculationOwnHouseHistoryDetail++;
                    }

                }else{
                    log.info("계산이력 저장 오류 > 계산이력ID : NULL");
                }

            }catch(Exception e){
                log.info("계산이력 저장 오류 > " + e.getMessage());
            }

            return calcHistoryId;
        }

        // 양도소득세 계산결과 텍스트 데이터 가져오기
        private String getCalculationResultTextData(CalculationSellResultRequest calculationSellResultRequest,
                                                    List<CalculationSellOneResult> calculationSellResultOneList,
                                                    List<String> commentaryList){

            log.info(">>> CalculationBranch getCalculationResultTextData - 양도소득세 계산결과 텍스트 데이터 가져오기");

            House sellHouse = houseUtil.findSelectedHouse(calculationSellResultRequest.getHouseId());

            StringBuilder textData = new StringBuilder(EMPTY);

            String houseTypeName = EMPTY;
            // 주택유형(1:아파트 2:연립,다가구 3:입주권 4:단독주택,다세대 5:분양권(주택) 6:주택)
            if(ONE.equals(sellHouse.getHouseType())){
                houseTypeName = "아파트";
            }else if(TWO.equals(sellHouse.getHouseType())){
                houseTypeName = "연립·다가구";
            }else if(THREE.equals(sellHouse.getHouseType())){
                houseTypeName = "입주권";
            }else if(FOUR.equals(sellHouse.getHouseType())){
                houseTypeName = "단독주택·다세대";
            }else if(FIVE.equals(sellHouse.getHouseType())){
                houseTypeName = "분양권(주택)";
            }else{
                houseTypeName = "주택";
            }

            textData.append("■ 양도소득세 계산 결과").append(NEW_LINE).append(NEW_LINE);
            textData.append("* 계산일시 : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append(NEW_LINE).append(NEW_LINE);
            textData.append("1. 양도 주택 정보").append(NEW_LINE);
            textData.append("  - 주택유형 : ").append(houseTypeName).append(NEW_LINE);
            textData.append("  - 주택명 : ").append(sellHouse.getHouseName()).append(NEW_LINE);
            textData.append("  - 상세주소 : ").append(sellHouse.getDetailAdr()).append(NEW_LINE);
            textData.append("  - 지번주소 : ").append(sellHouse.getJibunAddr()).append(NEW_LINE);
            textData.append("  - 도로명주소 : ").append(sellHouse.getRoadAddr()).append(NEW_LINE).append(NEW_LINE);

            DecimalFormat df = new DecimalFormat("###,###");
            CalculationSellOneResult calculationSellOneResult = null;

            if(calculationSellResultOneList != null && !calculationSellResultOneList.isEmpty()){
                textData.append("2. 계산결과").append(NEW_LINE);
                for(int i=0; i<calculationSellResultOneList.size(); i++){
                    calculationSellOneResult = calculationSellResultOneList.get(i);
                    textData.append(SPACE).append(i+1).append(") 소유자").append(i+1).append("(지분율 : ").append(sellHouse.getUserProportion()).append("%)").append(NEW_LINE);
                    textData.append("  - 총 납부세액 : ").append(df.format(Long.parseLong(calculationSellOneResult.getTotalTaxPrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 양도소득세 : ").append(df.format(Long.parseLong(calculationSellOneResult.getSellTaxPrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 지방소득세 : ").append(df.format(Long.parseLong(calculationSellOneResult.getLocalTaxPrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 양도금액(").append("지분비율 ").append(sellHouse.getUserProportion()).append("%) : ").append(df.format(Long.parseLong(calculationSellOneResult.getSellPrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 취득금액(").append("지분비율 ").append(sellHouse.getUserProportion()).append("%) : ").append(df.format(Long.parseLong(calculationSellOneResult.getBuyPrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 필요경비(").append("지분비율 ").append(sellHouse.getUserProportion()).append("%) : ").append(df.format(Long.parseLong(calculationSellOneResult.getNecExpensePrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 양도차익 : ").append(df.format(Long.parseLong(calculationSellOneResult.getSellProfitPrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 비과세 양도차익 : ").append(df.format(Long.parseLong(calculationSellOneResult.getNonTaxablePrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 과세대상 양도차익 : ").append(df.format(Long.parseLong(calculationSellOneResult.getTaxablePrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 장기보유특별공제 : ").append(df.format(Long.parseLong(calculationSellOneResult.getLongDeductionPrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 양도소득금액 : ").append(df.format(Long.parseLong(calculationSellOneResult.getSellIncomePrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 기본공제 : ").append(df.format(Long.parseLong(calculationSellOneResult.getBasicDeductionPrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 과세표준 : ").append(df.format(Long.parseLong(calculationSellOneResult.getTaxableStdPrice()))).append("원").append(NEW_LINE);
                    textData.append("  - 양도소득세율 : ").append(calculationSellOneResult.getSellTaxRate()).append("%").append(NEW_LINE);
                    textData.append("  - 지방소득세율 : ").append(calculationSellOneResult.getLocalTaxRate()).append("%").append(NEW_LINE);
                    textData.append("  - 누진공제 : ").append(df.format(Long.parseLong(calculationSellOneResult.getProgDeductionPrice()))).append("원").append(NEW_LINE);
                }
                textData.append(NEW_LINE);
            }

            textData.append("3. 주의").append(NEW_LINE);
            textData.append(" 1) 지금 보시는 세금 계산 결과는 법적 효력이 없으므로 정확한 세금 납부를 위해서는 전문가에게 상담을 추천해요.").append(NEW_LINE).append(NEW_LINE);

            if(commentaryList != null && !commentaryList.isEmpty()){
                textData.append("4. 해설").append(NEW_LINE);
                for(int i=0; i<commentaryList.size(); i++){
                    textData.append(SPACE).append(i+1).append(")").append(SPACE).append(commentaryList.get(i)).append(NEW_LINE);
                }
            }

            return textData.toString();
        }

        // 양도소득세 해설 리스트 가져오기
        private List<String> getCalculationSellCommentaryList(CalculationSellResultRequest calculationSellResultRequest, String taxRateCode, String dedCode, List<CalculationSellOneResult> calculationSellResultOneList){
            log.info(">>> CalculationBranch getCalculationSellCommentaryList - 양도소득세 해설 리스트 가져오기");

            // 양도주택
            House sellHouse = houseUtil.findSelectedHouse(calculationSellResultRequest.getHouseId());

            // 보유주택 수
            long ownHouseCount = houseUtil.countOwnHouse();

            List<CalculationProcess> calculationProcessList = null;
            String variableData = EMPTY;
            long variablePrice = 0;

            // 해설 리스트
            List<String> commentaryList = new ArrayList<>();

            // 1.공동명의 선택 시
            if(!sellHouse.getUserProportion().equals(100)){
                log.info("(Commentary Add) 1.공동명의 선택 시");
                commentaryList.add("입력하신 필요경비에는 지분율이 적용되지 않으니 지분율이 적용된 경비나 실제 해당 납세자가 지출한 경비를 기재해주세요.");
                commentaryList.add("같은 세대의 공동명의 취득이 아닌, 각각 다른 세대의 공동명의 취득인 경우나 각각 공동명의자의 취득일이 다른 경우에는 결과값이 다를 수 있습니다.");
            }

            // 3.양도시점 1주택자 양도가액 12억 초과 시
            calculationProcessList = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "015")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            variableData = StringUtils.defaultString(calculationProcessList.get(0).getVariableData(), ZERO);
            variablePrice = Long.parseLong(variableData);   // 12억

            if(houseUtil.countOwnHouse() == 1 && calculationSellResultRequest.getSellPrice() > variablePrice){
                log.info("(Commentary Add) 2.양도시점 1주택자 양도가액 12억 초과 시");
                commentaryList.add("1세대 1주택 비과세 대상 중 12억원 초과 고가주택을 거래하는 경우에는 초과분에 대한 비율만큼 양도세가 부과돼요.");
            }

            // 4.양도하려는 물건의 종류가 분양권/입주권인 경우
            // 9.양도하려는 물건의 종류가 분양권인 경우
            if(THREE.equals(sellHouse.getHouseType()) || FIVE.equals(sellHouse.getHouseType())){
                log.info("(Commentary Add) 3.양도하려는 물건의 종류가 분양권/입주권인 경우");
                commentaryList.add("분양권 및 입주권은 장기보유특별공제를 적용받지 않아요.");

                if(FIVE.equals(sellHouse.getHouseType())){
                    log.info("(Commentary Add) 9.양도하려는 물건의 종류가 분양권인 경우");
                    commentaryList.add("분양권은 최초분양계약일로부터 보유기간을 산정하며 비과세 혜택이 없어요.");
                }
            }

            // 5.항상
            log.info("(Commentary Add) 4.항상");
            commentaryList.add("연 1회 인별 기본공제 250만원이 적용돼요.");

            // 6.일반 2주택자 이상인 경우
            if(ownHouseCount >= 2){
                log.info("(Commentary Add) 6.일반 2주택자 이상인 경우");
                commentaryList.add("2022.05.10. 다주택자 양도소득세 중과배제 조치 시행으로 22.05.10. - 25.05.09. 까지 한시적으로 적용 배제돼요. " +
                        "해당 기간 외의 거래에 대해서는 중과 대상(20%, 30%)이며, 장기보유특별공제도 적용되지 않아요.");
            }

            // 7.(양도주택)취득시점 조정대상지역 해당
            if(checkAdjustmentTargetArea(StringUtils.defaultString(sellHouse.getJibunAddr()), sellHouse.getBuyDate())){
                log.info("(Commentary Add) 7.(양도주택)취득시점 조정대상지역 해당");
                commentaryList.add("2017.08.03. 이후 조정대상지역(구입 당시 기준)에서 구입한 주택을 비과세받고자 하는 경우 2년의 의무거주기간 요건을 충족해야 해요.");
            }

            // 8.상생임대인에 해당되는 경우
            // 15.거주기간을 입력받았을 경우
            List<CalculationAdditionalAnswerRequest> additionalAnswerList = calculationSellResultRequest.getAdditionalAnswerList();
            for(CalculationAdditionalAnswerRequest answer : additionalAnswerList){
                if(Q_0006.equals(answer.getQuestionId())){
                    if(ANSWER_VALUE_01.equals(answer.getAnswerValue())){
                        log.info("(Commentary Add) 8.상생임대인에 해당되는 경우");
                        commentaryList.add("조정대상지역 내 주택에 대해 거주요건을 충족하지 못하였으나 상생임대인에 해당되어 비과세 적용이 가능해요.");
                    }
                }else if(PERIOD_TYPE_DIAL.equals(answer.getQuestionId()) || PERIOD_TYPE_CERT.equals(answer.getQuestionId())){
                    long stayPeriodYear = 0;
                    long stayPeriodMonth = 0;
                    Map<String, Object> stayPeriodMap = getStayPeriodYearAndMonth(answer.getAnswerValue());
                    if(stayPeriodMap.containsKey(STAY_PERIOD_YEAR)){
                        stayPeriodYear = (long)stayPeriodMap.get(STAY_PERIOD_YEAR);
                    }
                    if(stayPeriodMap.containsKey(STAY_PERIOD_MONTH)){
                        stayPeriodMonth = (long)stayPeriodMap.get(STAY_PERIOD_MONTH);
                    }

                    if(stayPeriodYear != 0 || stayPeriodMonth != 0){
                        log.info("(Commentary Add) 15.거주기간을 입력받았을 경우");
                        StringBuilder tempCommentary = new StringBuilder(EMPTY);
                        tempCommentary.delete(0, tempCommentary.length());
                        tempCommentary.append("양도하실 주택의 거주기간은 총 ");
                        if(stayPeriodYear != 0) tempCommentary.append(stayPeriodYear).append("년 ");
                        if(stayPeriodMonth != 0) tempCommentary.append(stayPeriodMonth).append("개월 ");
                        tempCommentary.append("이에요");
                        commentaryList.add(tempCommentary.toString());
                    }
                }
            }

            // 10.1주택, 비과세 대상인 경우
            if(ownHouseCount == 1 && NONE_TAX_RATE_CODE.equals(taxRateCode)){
                log.info("(Commentary Add) 10.1주택, 비과세 대상인 경우");
                commentaryList.add("주택은 잔금일(또는 등기일 중 빠른 날)로부터 2년 이상 보유한 이후 양도해야 비과세 적용이 가능해요.");
            }

            // 12.항상
            log.info("(Commentary Add) 12.항상");
            commentaryList.add("고객님은 양도하실 주택을 포함하여 현재 " + ownHouseCount + "채를 보유하고 있어요.");

            // 13.양도대상주택 포함하여 2주택이면서 일시적1가구2주택으로 인정되어 비과세를 적용받는경우
            if(ownHouseCount == 2 && NONE_TAX_RATE_CODE.equals(taxRateCode)){
                log.info("(Commentary Add) 13.양도대상주택 포함하여 2주택이면서 일시적1가구2주택으로 인정되어 비과세를 적용받는경우");
                commentaryList.add("고객님은 일시적1가구 2주택으로 인정되어 양도세 비과세 혜택을 적용 받을 수 있어요.");
            }

            // 14.항상
            if(calculationSellResultOneList != null){
                for(CalculationSellOneResult calculationSellOneResult : calculationSellResultOneList){
                    if(calculationSellOneResult.getSellTaxRate() != null && !calculationSellOneResult.getSellTaxRate().isBlank()){
                        log.info("(Commentary Add) 14.항상");
                        //commentaryList.add("양도하실 주택의 최종 세율은 " + calculationSellOneResult.getSellTaxRate() + "% 에요.");
                        commentaryList.add("양도하실 주택의 최종 세율은 양도소득세 " + calculationSellOneResult.getSellTaxRate() + "%, 지방소득세 " + calculationSellOneResult.getLocalTaxRate() + "% 에요.");
                        break;
                    }
                }
            }

            // 17.(양도주택)양도시점 조정대상지역 해당
            if(checkAdjustmentTargetArea(StringUtils.defaultString(sellHouse.getJibunAddr()), calculationSellResultRequest.getSellDate())){
                log.info("(Commentary Add) 17.(양도주택)양도시점 조정대상지역 해당");
                commentaryList.add("양도하시려는 주택은 양도예정일 기준 조정대상에 해당해요.");
            }

            // 19.양도하려는 주택외에 주택수에 포함되는 분양권(2021.01.01 이후 취득)이 있는 경우
            List<House> houseList = houseUtil.findOwnHouseList();
            if(houseList != null && houseUtil.countOwnHouse() > 1 && houseList.size() > 1){
                for(House house : houseList){
                    if(!house.getHouseId().equals(sellHouse.getHouseId())){
                        LocalDate specificDate = LocalDate.parse("20210101", DateTimeFormatter.ofPattern("yyyyMMdd"));
                        if(FIVE.equals(house.getHouseType()) && house.getBuyDate().isAfter(specificDate)){
                            log.info("(Commentary Add) 19.양도하려는 주택외에 주택수에 포함되는 분양권(2021.01.01 이후 취득)이 있는 경우");
                            commentaryList.add("2021.01.01 이후 취득 한 분양권을 보유하고 계시며, 해당 분양권은 보유주택수에 포함돼요.");
                        }
                    }
                }
            }

            return commentaryList;
        }

        private CalculationSellResultResponse getCalculationSellResultResponseTest(){
            log.info(">>> CalculationBranch getCalculationSellResult - 양도소득세 계산 수행 테스트");

            // 양도소득세 계산 결과 세팅
            List<CalculationSellOneResult> calculationSellResultOneList = new ArrayList<>();

            calculationSellResultOneList.add(
                    CalculationSellOneResult.builder()
                            .buyPrice("950000000")
                            .buyDate("2021-04-22")
                            .sellPrice("1300000000")
                            .sellDate("2024-05-24")
                            .necExpensePrice("10000000")
                            .sellProfitPrice("340000000")
                            .retentionPeriod("3년 1개월")
                            .nonTaxablePrice("313846154")
                            .taxablePrice("26153846")
                            .longDeductionPrice("1569231")
                            .sellIncomePrice("24584615")
                            .basicDeductionPrice("2500000")
                            .taxableStdPrice("22084615")
                            .sellTaxRate("15%")
                            .localTaxRate("1.5%")
                            .progDeductionPrice("1260000")
                            .sellTaxPrice("2052692")
                            .localTaxPrice("205269")
                            .totalTaxPrice("2257962")
                            .build());

            return CalculationSellResultResponse.builder()
                    .listCnt(1)
                    .list(calculationSellResultOneList)
                    .build();

        }

        // (양도소득세)일반세율 계산
        private double calculateGeneralTaxRate(long taxableStdPrice){
            double taxRate = 0;

            // TODO. 추후 DB로 구현
            if(taxableStdPrice <= 14000000){
                taxRate = 0.06;
            }else if(taxableStdPrice <= 50000000){
                taxRate = 0.15;
            }else if(taxableStdPrice <= 88000000){
                taxRate = 0.24;
            }else if(taxableStdPrice <= 150000000){
                taxRate = 0.35;
            }else if(taxableStdPrice <= 300000000){
                taxRate = 0.38;
            }else if(taxableStdPrice <= 500000000){
                taxRate = 0.40;
            }else if(taxableStdPrice <= 1000000000){
                taxRate = 0.42;
            }else {
                taxRate = 0.45;
            }

            return taxRate;
        }

        // (양도소득세)누진공제금액 계산
        private long calculateProgDeductionPrice(long taxableStdPrice){
            long progDeductionPrice = 0;

            // TODO. 추후 DB로 구현
            if(taxableStdPrice <= 14000000){
                //progDeductionPrice = 0;
            }else if(taxableStdPrice <= 50000000){
                progDeductionPrice = 1260000;
            }else if(taxableStdPrice <= 88000000){
                progDeductionPrice = 5760000;
            }else if(taxableStdPrice <= 150000000){
                progDeductionPrice = 15440000;
            }else if(taxableStdPrice <= 300000000){
                progDeductionPrice = 19940000;
            }else if(taxableStdPrice <= 500000000){
                progDeductionPrice = 25940000;
            }else if(taxableStdPrice <= 1000000000){
                progDeductionPrice = 35940000;
            }else {
                progDeductionPrice = 65940000;
            }

            return progDeductionPrice;
        }

        // 공제율 계산
        private double calculateDeductionRate(DeductionInfo deductionInfo, Long rPeriod, Long sPeriod){
            double dedRate1 = 0;
            double dedRate2 = 0;
            double finalDedRate = 0;

            String dedMethod = StringUtils.defaultString(deductionInfo.getDedMethod());     // 공제함수

            String dedTarget1 = StringUtils.defaultString(deductionInfo.getDedTarget1());   // 공제대상1
            String unit1 = StringUtils.defaultString(deductionInfo.getUnit1());             // 단위1
            double unitDedRate1 = deductionInfo.getUnitDedRate1();                          // 단위공제율1
            int limitYear1 = deductionInfo.getLimitYear1();                                 // 한도연수1
            double limitDedRate1 = deductionInfo.getLimitDedRate1();                        // 한도공제율1

            String dedTarget2 = StringUtils.defaultString(deductionInfo.getDedTarget1());   // 공제대상2
            String unit2 = StringUtils.defaultString(deductionInfo.getUnit1());             // 단위2
            double unitDedRate2 = deductionInfo.getUnitDedRate1();                          // 단위공제율2
            int limitYear2 = deductionInfo.getLimitYear1();                                 // 한도연수2
            double limitDedRate2 = deductionInfo.getLimitDedRate1();                        // 한도공제율2

            long retentionPeriodYear = (rPeriod != null) ? rPeriod : 0;
            long stayPeriodYear = (sPeriod != null) ? sPeriod : 0;

            if(!EMPTY.equals(dedTarget1)){
                if(UNIT_1YEAR.equals(unit1)){
                    if(DEDUCTION_TARGET_RETENTION.equals(dedTarget1)){
                        dedRate1 = Math.min(retentionPeriodYear * unitDedRate1, limitDedRate1);
                    }else if(DEDUCTION_TARGET_STAY.equals(dedTarget1)){
                        dedRate1 = Math.min(stayPeriodYear * unitDedRate1, limitDedRate1);
                    }
                }
            }

            if(!EMPTY.equals(dedTarget2)){
                if(UNIT_1YEAR.equals(unit2)){
                    if(DEDUCTION_TARGET_RETENTION.equals(dedTarget2)){
                        dedRate2 = Math.min(retentionPeriodYear * unitDedRate2, limitDedRate2);
                    }else if(DEDUCTION_TARGET_STAY.equals(dedTarget2)){
                        dedRate2 = Math.min(stayPeriodYear * unitDedRate2, limitDedRate2);
                    }
                }
            }

            if(!EMPTY.equals(dedMethod)){
                if(SUM.equals(dedMethod)){
                    // SUM : 두 공제율 합산
                    finalDedRate = dedRate1 + dedRate2;
                }else{
                    finalDedRate = dedRate1;    // 기본
                }
            }else{
                finalDedRate = dedRate1;        // 기본
            }

            if(finalDedRate > 1) finalDedRate = 1;  // 최종 공제율은 1을 넘지 않는다(100%)

            return finalDedRate;
        }

        // 조정대상지역 여부 체크
        private boolean checkAdjustmentTargetArea(String address, LocalDate date){
            boolean isAdjustmentTargetArea = false;

            HouseAddressDto houseAddressDto = houseAddressService.separateAddress(address);

            // 지번주소
            if(houseAddressDto.getAddressType() == 1){
                List<String> searchAddress = houseAddressDto.getSearchAddress();
                if(searchAddress != null && searchAddress.get(0) != null){
                    String[] keywordArr = searchAddress.get(0).split(SPACE);
                    StringBuilder keywordAssemble = new StringBuilder(EMPTY);
                    List<AdjustmentTargetAreaInfo> adjustmentTargetAreaInfoList = new ArrayList<>();
                    boolean isFindAddress = false;

                    for(int i=0; i<keywordArr.length; i++){
                        String keyword = keywordArr[i];

                        if(!EMPTY.contentEquals(keywordAssemble)){
                            keywordAssemble.append(SPACE);
                        }

                        if(keyword != null && !EMPTY.equals(keyword)){
                            keywordAssemble.append(keyword);
                        }

                        if(i>0){
                            adjustmentTargetAreaInfoList = adjustmentTargetAreaRepository.findByTargetAreaStartingWith(keywordAssemble.toString());
                            if(adjustmentTargetAreaInfoList != null){
                                // 조회 결과가 1개인 경우(조회결과가 1개가 나올 때까지 반복)
                                if(adjustmentTargetAreaInfoList.size() == 1){
                                    isFindAddress = true;
                                    break;
                                }else if(adjustmentTargetAreaInfoList.isEmpty()){
                                    break;
                                }
                            }
                        }
                    }

                    if(isFindAddress){
                        LocalDate startDate = null;
                        LocalDate endDate = null;

                        AdjustmentTargetAreaInfo adjustmentTargetAreaInfo = adjustmentTargetAreaInfoList.get(0);

                        if(adjustmentTargetAreaInfo != null){
                            if(adjustmentTargetAreaInfo.getStartDate() != null){
                                startDate = adjustmentTargetAreaInfo.getStartDate();
                            }
                            if(adjustmentTargetAreaInfo.getEndDate() != null){
                                endDate = adjustmentTargetAreaInfo.getEndDate();
                            }

                            if(startDate != null){
                                if(endDate != null){
                                    if((date.isEqual(startDate) || date.isAfter(startDate)) && (date.isEqual(endDate) || date.isBefore(endDate))){
                                        isAdjustmentTargetArea = true;
                                    }
                                }else{
                                    if(date.isEqual(startDate) || date.isAfter(startDate)){
                                        isAdjustmentTargetArea = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }else{
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "조정대상지역 체크 중 오류가 발생했습니다.(지번주소 아님)");
            }

            return isAdjustmentTargetArea;
        }

        // selectNo 조건 부합 여부 체크
        private boolean checkSelectNoCondition(int dataType, String inputData1, String inputData2, String variableData, String dataMethod){
            boolean result = false;
            // 금액
            if(DATA_TYPE_PRICE == dataType){
                if(inputData1 == null || variableData == null || dataMethod == null){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 계산을 위한 파라미터가 올바르지 않습니다.");
                }

                long inputPrice = Long.parseLong(StringUtils.defaultString(inputData1, ZERO));
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
                if(inputData1 == null || variableData == null || dataMethod == null){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 계산을 위한 파라미터가 올바르지 않습니다.");
                }

                LocalDate inputDate = LocalDate.parse(inputData1, DateTimeFormatter.ofPattern("yyyyMMdd"));
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
            }else if(DATA_TYPE_PERIOD == dataType){
                if(inputData1 == null || inputData2 == null || variableData == null || dataMethod == null){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 계산을 위한 파라미터가 올바르지 않습니다.");
                }

                long variablePeriod = Long.parseLong(StringUtils.defaultString(variableData, ZERO)) * PERIOD_YEAR;
                long inputPeriod = 0;

                if(inputData1.length() == 8 && inputData2.length() == 8){
                    LocalDate startDate = LocalDate.parse(inputData1, DateTimeFormatter.ofPattern("yyyyMMdd"));
                    LocalDate endDate = LocalDate.parse(inputData2, DateTimeFormatter.ofPattern("yyyyMMdd"));
                    inputPeriod = ChronoUnit.DAYS.between(startDate, endDate);  // 기간(일)
                }else{
                    long stayPeriodYear = Long.parseLong(inputData1);
                    long stayPeriodMonth = Long.parseLong(inputData2);
                    inputPeriod = stayPeriodYear * PERIOD_YEAR + stayPeriodMonth * PERIOD_MONTH + 2;    // 대략적인 계산
                }

                // n년이 된 날 이내
                if(WITHIN.equals(dataMethod)){
                    if(inputPeriod <= variablePeriod){
                        result = true;
                    }
                }
                // n년이 된 날 전날 이내
                else if(WITHIN_YST.equals(dataMethod)){
                    if(inputPeriod <= variablePeriod - 1){
                        result = true;
                    }
                }
                // n년이 된 날 다음날 이내
                else if(WITHIN_TMR.equals(dataMethod)){
                    if(inputPeriod <= variablePeriod + 1){
                        result = true;
                    }
                }
                // n년이 된 날 이후
                else if(NOT_WITHIN.equals(dataMethod)){
                    if(inputPeriod > variablePeriod){
                        result = true;
                    }
                }
                // n년이 된 날 전날 이후
                else if(NOT_WITHIN_YST.equals(dataMethod)){
                    if(inputPeriod > variablePeriod - 1){
                        result = true;
                    }
                }
                // n년이 된 날 다음날 이후
                else if(NOT_WITHIN_TMR.equals(dataMethod)){
                    if(inputPeriod > variablePeriod + 1){
                        result = true;
                    }
                }
            }

            return result;
        }

        // 종전 또는 신규주택 조회
        private House getOldOrNewHouse(List<House> userHouseList, boolean isNew){
            House house = null;

            if(userHouseList != null && userHouseList.size() == 2){
                LocalDate buyDate1 = userHouseList.get(0).getBuyDate();
                LocalDate buyDate2 = userHouseList.get(1).getBuyDate();

                if(buyDate1 != null && buyDate2 != null){
                    // 두 주택의 취득일이 같지 않은 경우
                    if(!buyDate1.equals(buyDate2)){
                        // 신규주택 응답 요청
                        if(isNew){
                            if(buyDate1.isBefore(buyDate2)){
                                house = userHouseList.get(1);
                            }else{
                                house = userHouseList.get(0);
                            }
                        }
                        // 종전주택 응답 요청
                        else{
                            if(buyDate1.isBefore(buyDate2)){
                                house = userHouseList.get(0);
                            }else{
                                house = userHouseList.get(1);
                            }
                        }
                    }
                    // 두 주택의 취득일이 같은 경우(순서대로 세팅)
                    else{
                        if(isNew){
                            house = userHouseList.get(0);
                        }else{
                            house = userHouseList.get(1);
                        }
                    }
                }
            }

            return house;
        }

        // 실거주기간 입력 내용 분리(년, 월로 분리)
        private Map<String, Object> getStayPeriodYearAndMonth(String stayPeriodStr){
            Map<String, Object> resultMap = new HashMap<String, Object>();
            long stayPeriodYear = 0;
            long stayPeriodMonth = 0;

            String stayPeriodTotalStr = EMPTY;
            String stayPeriodYearStr = EMPTY;
            String stayPeriodMonthStr = EMPTY;

            stayPeriodTotalStr = StringUtils.defaultString(stayPeriodStr);
            if(!stayPeriodTotalStr.contains("년") && !stayPeriodTotalStr.contains("개월")){
                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도주택의 실거주기간 정보가 입력되지 않았습니다.");
            }else{
                try{
                    // 1년 미만(ex:5개월)
                    if(!stayPeriodTotalStr.contains("년")){
                        stayPeriodMonthStr = stayPeriodTotalStr.replace("개월", EMPTY);
                        stayPeriodMonth = Long.parseLong(stayPeriodMonthStr);
                    }
                    // 1년 이상
                    else{
                        // 개월 없음(ex:2년)
                        if(!stayPeriodTotalStr.contains("개월")){
                            stayPeriodYearStr = stayPeriodTotalStr.replace("년", EMPTY);
                            stayPeriodYear = Long.parseLong(stayPeriodYearStr);
                        }
                        // 개월 있음(ex:2년 10개월)
                        else{
                            String[] stayPeriodArr = stayPeriodTotalStr.split(SPACE);
                            if(stayPeriodArr.length == 2){
                                stayPeriodYearStr = stayPeriodArr[0].replace("년", EMPTY);
                                stayPeriodMonthStr = stayPeriodArr[1].replace("개월", EMPTY);
                                stayPeriodYear = Long.parseLong(stayPeriodYearStr);
                                stayPeriodMonth = Long.parseLong(stayPeriodMonthStr);
                            }else{
                                throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도주택의 실거주기간 정보가 입력되지 않았습니다.");
                            }
                        }
                    }
                }catch(NumberFormatException ne){
                    throw new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도주택의 실거주기간 정보가 올바르게 입력되지 않았습니다.");
                }
            }

            resultMap.put("stayPeriodYear", stayPeriodYear);
            resultMap.put("stayPeriodMonth", stayPeriodMonth);

            return resultMap;
        }
    }
}