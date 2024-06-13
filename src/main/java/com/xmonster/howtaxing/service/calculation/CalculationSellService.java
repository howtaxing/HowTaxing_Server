package com.xmonster.howtaxing.service.calculation;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultRequest;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultResponse;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultResponse.CalculationSellOneResult;
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
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CalculationSellService {
    private final HouseAddressService houseAddressService;

    private final CalculationSellResultRepository calculationSellResultRepository;
    private final CalculationProcessRepository calculationProcessRepository;
    private final TaxRateInfoRepository taxRateInfoRepository;
    private final DeductionInfoRepository deductionInfoRepository;
    private final HouseRepository houseRepository;

    private final UserUtil userUtil;
    private final HouseUtil houseUtil;

    private Class<?> calculationBranchClass;
    private CalculationBranch target;

    // 양도소득세 계산 결과 조회
    public Object getCalculationSellResult(CalculationSellResultRequest calculationSellResultRequest){
        log.info(">> [Service]CalculationSellService getCalculationSellResult - 양도소득세 계산 결과 조회");

        // 요청 데이터 유효성 검증
        validationCheckRequestData(calculationSellResultRequest);

        // 양도주택정보
        House house = houseRepository.findByHouseId(calculationSellResultRequest.getHouseId())
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR));

        // 분기 메소드
        try{
            calculationBranchClass = Class.forName("com.xmonster.howtaxing.service.calculation.CalculationSellService$CalculationBranch");
            target = new CalculationBranch();

            Method method = calculationBranchClass.getMethod("calculationStart", CalculationSellResultRequest.class, House.class);

            Object result = method.invoke(target, calculationSellResultRequest, house);

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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
            } else if(THREE.equals(house.getHouseType())){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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

            // 보유주택 수
            long ownHouseCount = getOwnHouseCount();

            // 1주택
            if(ownHouseCount == 1){
                selectNo = 1;
            }
            // 2주택
            else if(ownHouseCount == 2){
                selectNo = 2;
            }
            // 3주택 이상
            else if(ownHouseCount >= 3){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(house.getRoadAddr()));

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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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

            Long stayPeriodYear = calculationSellResultRequest.getStayPeriodYear();
            Long stayPeriodMonth = calculationSellResultRequest.getStayPeriodMonth();

            if(stayPeriodYear == null || stayPeriodMonth == null){
                stayPeriodYear = 0L;
                stayPeriodMonth = 0L;
            }

            log.info("stayPeriodYear : " + stayPeriodYear + "년");
            log.info("stayPeriodMonth : " + stayPeriodMonth + "개월");

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, stayPeriodYear.toString(), stayPeriodMonth.toString(), variableData, dataMethod)){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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

            // 상생임대인여부
            boolean isWWLandLord = false;

            if(calculationSellResultRequest.getIsWWLandLord() != null){
                isWWLandLord = calculationSellResultRequest.getIsWWLandLord();
            }

            if(isWWLandLord){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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

            Long stayPeriodYear = calculationSellResultRequest.getStayPeriodYear();
            Long stayPeriodMonth = calculationSellResultRequest.getStayPeriodMonth();

            if(stayPeriodYear == null || stayPeriodMonth == null){
                stayPeriodYear = 0L;
                stayPeriodMonth = 0L;
            }

            log.info("stayPeriodYear : " + stayPeriodYear + "년");
            log.info("stayPeriodMonth : " + stayPeriodMonth + "개월");

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, stayPeriodYear.toString(), stayPeriodMonth.toString(), variableData, dataMethod)){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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

            // 취득일 동일 여부
            boolean isBuyDateSame = false;
            
            // 보유주택 수가 2개인 경우에만 확인 가능(그 외엔 false)
            if(getOwnHouseCount() == 2){
                List<House> houseList = houseRepository.findByUserId(userUtil.findCurrentUser().getId()).orElse(null);
                if(houseList != null && houseList.size() == 2){
                    LocalDate buyDate1 = houseList.get(0).getBuyDate();
                    LocalDate buyDate2 = houseList.get(1).getBuyDate();

                    if(buyDate1.isEqual(buyDate2)){
                        isBuyDateSame = true;
                    }
                }
            }

            if(isBuyDateSame){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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

            // 신규주택여부
            boolean isNewHouse = false;

            // 보유주택 수가 2개인 경우에만 확인 가능(그 외엔 false)
            if(getOwnHouseCount() == 2){
                List<House> houseList = houseRepository.findByUserId(userUtil.findCurrentUser().getId()).orElse(null);
                if(houseList != null && houseList.size() == 2){
                    LocalDate buyDate1 = houseList.get(0).getBuyDate();
                    LocalDate buyDate2 = houseList.get(1).getBuyDate();
                    LocalDate buyDate = house.getBuyDate();

                    if(buyDate1.isBefore(buyDate2)){
                        if(buyDate2.equals(buyDate)){
                            isNewHouse = true;
                        }
                    }else if(buyDate1.isAfter(buyDate2)){
                        if(buyDate1.equals(buyDate)){
                            isNewHouse = true;
                        }
                    }
                }
            }

            // 신규주택
            if(isNewHouse){
                selectNo = 2;
            }
            // 종전주택
            else{
                selectNo = 1;
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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

            // 주택유형(6:주택이 기본값)
            String newHouseType = SIX;

            // 보유주택 수가 2개인 경우에만 확인 가능(그 외엔 false)
            if(getOwnHouseCount() == 2){
                List<House> houseList = houseRepository.findByUserId(userUtil.findCurrentUser().getId()).orElse(null);
                if(houseList != null && houseList.size() == 2){
                    LocalDate buyDate1 = houseList.get(0).getBuyDate();
                    LocalDate buyDate2 = houseList.get(1).getBuyDate();
                    LocalDate buyDate = house.getBuyDate();

                    if(buyDate1.isBefore(buyDate2)){
                        if(buyDate1.equals(buyDate)){
                            newHouseType = houseList.get(1).getHouseType();
                        }else{
                            newHouseType = houseList.get(0).getHouseType();
                        }
                    }else if(buyDate1.isAfter(buyDate2)){
                        if(buyDate1.equals(buyDate)){
                            newHouseType = houseList.get(0).getHouseType();
                        }else{
                            newHouseType = houseList.get(1).getHouseType();
                        }
                    }
                }
            }

            if(FIVE.equals(newHouseType) || THREE.equals(newHouseType)){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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

            LocalDate lastHouseBuyDate = null;
            LocalDate newHouseBuyDate = null;

            // 보유주택 수가 2개인 경우에만 확인 가능
            if(getOwnHouseCount() == 2){
                List<House> houseList = houseRepository.findByUserId(userUtil.findCurrentUser().getId()).orElse(null);
                if(houseList != null && houseList.size() == 2){
                    LocalDate buyDate1 = houseList.get(0).getBuyDate();
                    LocalDate buyDate2 = houseList.get(1).getBuyDate();
                    LocalDate buyDate = house.getBuyDate();

                    if(buyDate1.isBefore(buyDate2)){
                        if(buyDate1.equals(buyDate)){
                            lastHouseBuyDate = houseList.get(0).getBuyDate();
                            newHouseBuyDate = houseList.get(1).getBuyDate();
                        }else {
                            lastHouseBuyDate = houseList.get(1).getBuyDate();
                            newHouseBuyDate = houseList.get(0).getBuyDate();
                        }
                    }else if(buyDate1.isAfter(buyDate2)){
                        if(buyDate1.equals(buyDate)){
                            lastHouseBuyDate = houseList.get(1).getBuyDate();
                            newHouseBuyDate = houseList.get(0).getBuyDate();
                        }else{
                            lastHouseBuyDate = houseList.get(0).getBuyDate();
                            newHouseBuyDate = houseList.get(1).getBuyDate();
                        }
                    }
                }
            }

            String lastHouseBuyDateStr = (lastHouseBuyDate != null) ? lastHouseBuyDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String newHouseBuyDateStr = (newHouseBuyDate != null) ? newHouseBuyDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("lastHouseBuyDateStr : " + lastHouseBuyDateStr);
            log.info("newHouseBuyDateStr : " + newHouseBuyDateStr);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, lastHouseBuyDateStr, newHouseBuyDateStr, variableData, dataMethod)){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 023
         * 분기명 : 종전주택 보유기간
         * 분기설명 : (2주택)종전주택을 취득일로부터 [2]년이 된 날 이후 매도 (예정)여부
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

            Map<String, Object> planAnswer = calculationSellResultRequest.getPlanAnswer();

            boolean answer = false;
            if(planAnswer != null && planAnswer.containsKey(A_0001) && planAnswer.get(A_0001) != null){
                answer = (boolean)planAnswer.get(A_0001);
                log.info(A_0001 + " : " + answer);
            }

            if(answer){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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

            Map<String, Object> planAnswer = calculationSellResultRequest.getPlanAnswer();

            boolean answer = false;
            if(planAnswer != null && planAnswer.containsKey(A_0002) && planAnswer.get(A_0002) != null){
                answer = (boolean)planAnswer.get(A_0002);
                log.info(A_0002 + " : " + answer);
            }

            if(answer){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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

            LocalDate lastHouseBuyDate = null;
            LocalDate newHouseBuyDate = null;

            // 보유주택 수가 2개인 경우에만 확인 가능
            if(getOwnHouseCount() == 2){
                List<House> houseList = houseRepository.findByUserId(userUtil.findCurrentUser().getId()).orElse(null);
                if(houseList != null && houseList.size() == 2){
                    LocalDate buyDate1 = houseList.get(0).getBuyDate();
                    LocalDate buyDate2 = houseList.get(1).getBuyDate();
                    LocalDate buyDate = house.getBuyDate();

                    if(buyDate1.isBefore(buyDate2)){
                        if(buyDate1.equals(buyDate)){
                            lastHouseBuyDate = houseList.get(0).getBuyDate();
                            newHouseBuyDate = houseList.get(1).getBuyDate();
                        }else {
                            lastHouseBuyDate = houseList.get(1).getBuyDate();
                            newHouseBuyDate = houseList.get(0).getBuyDate();
                        }
                    }else if(buyDate1.isAfter(buyDate2)){
                        if(buyDate1.equals(buyDate)){
                            lastHouseBuyDate = houseList.get(1).getBuyDate();
                            newHouseBuyDate = houseList.get(0).getBuyDate();
                        }else{
                            lastHouseBuyDate = houseList.get(0).getBuyDate();
                            newHouseBuyDate = houseList.get(1).getBuyDate();
                        }
                    }
                }
            }

            String lastHouseBuyDateStr = (lastHouseBuyDate != null) ? lastHouseBuyDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;
            String newHouseBuyDateStr = (newHouseBuyDate != null) ? newHouseBuyDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("lastHouseBuyDateStr : " + lastHouseBuyDateStr);
            log.info("newHouseBuyDateStr : " + newHouseBuyDateStr);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_PERIOD, lastHouseBuyDateStr, newHouseBuyDateStr, variableData, dataMethod)){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 026
         * 분기명 : 종전주택 보유기간
         * 분기설명 : (2주택)종전주택을 취득일로부터 [2]년이 된 날 이후 매도 (예정)여부
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

            Map<String, Object> planAnswer = calculationSellResultRequest.getPlanAnswer();

            boolean answer = false;
            if(planAnswer != null && planAnswer.containsKey(A_0001) && planAnswer.get(A_0001) != null){
                answer = (boolean)planAnswer.get(A_0001);
                log.info(A_0001 + " : " + answer);
            }

            if(answer){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 027
         * 분기명 : 취득일자 확인
         * 분기설명 : (2주택)종전주택 취득일 [2022.02.15] 이전 여부
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

            LocalDate lastHouseBuyDate = null;

            // 보유주택 수가 2개인 경우에만 확인 가능
            if(getOwnHouseCount() == 2){
                List<House> houseList = houseRepository.findByUserId(userUtil.findCurrentUser().getId()).orElse(null);
                if(houseList != null && houseList.size() == 2){
                    LocalDate buyDate1 = houseList.get(0).getBuyDate();
                    LocalDate buyDate2 = houseList.get(1).getBuyDate();
                    LocalDate buyDate = house.getBuyDate();

                    if(buyDate1.isBefore(buyDate2)){
                        if(buyDate1.equals(buyDate)){
                            lastHouseBuyDate = houseList.get(0).getBuyDate();
                        }else {
                            lastHouseBuyDate = houseList.get(1).getBuyDate();
                        }
                    }else if(buyDate1.isAfter(buyDate2)){
                        if(buyDate1.equals(buyDate)){
                            lastHouseBuyDate = houseList.get(1).getBuyDate();
                        }else{
                            lastHouseBuyDate = houseList.get(0).getBuyDate();
                        }
                    }
                }
            }

            String lastHouseBuyDateStr = (lastHouseBuyDate != null) ? lastHouseBuyDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) : EMPTY;

            log.info("lastHouseBuyDateStr : " + lastHouseBuyDateStr);

            for(CalculationProcess calculationProcess : list){
                String dataMethod = StringUtils.defaultString(calculationProcess.getDataMethod());
                String variableData = StringUtils.defaultString(calculationProcess.getVariableData(), ZERO);

                if(checkSelectNoCondition(DATA_TYPE_DATE, lastHouseBuyDateStr, EMPTY, variableData, dataMethod)){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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

            Map<String, Object> planAnswer = calculationSellResultRequest.getPlanAnswer();

            boolean answer = false;
            if(planAnswer != null && planAnswer.containsKey(A_0001) && planAnswer.get(A_0001) != null){
                answer = (boolean)planAnswer.get(A_0001);
                log.info(A_0001 + " : " + answer);
            }

            if(answer){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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

            Map<String, Object> planAnswer = calculationSellResultRequest.getPlanAnswer();

            boolean answer = false;
            if(planAnswer != null && planAnswer.containsKey(A_0002) && planAnswer.get(A_0002) != null){
                answer = (boolean)planAnswer.get(A_0002);
                log.info(A_0002 + " : " + answer);
            }

            if(answer){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        /**
         * 분기번호 : 030
         * 분기명 : 신규주택 거주 계획
         * 분기설명 : (2주택)신규주택에 주민등록초본상 전입일로부터 1년이 된날 이후까지 계속 거주 계획 여부(사용자 선택)
         */
        public CalculationSellResultResponse branchNo030(CalculationSellResultRequest calculationSellResultRequest, House house){
            log.info(">>> CalculationBranch branchNo029 - 양도소득세 분기번호 030 : 신규주택 거주 계획");

            CalculationSellResultResponse calculationSellResultResponse;
            boolean hasNext = false;
            String nextBranchNo = EMPTY;
            String taxRateCode = EMPTY;
            String dedCode = EMPTY;
            int selectNo = 0;

            List<CalculationProcess> list = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "030")
                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

            Map<String, Object> planAnswer = calculationSellResultRequest.getPlanAnswer();

            boolean answer = false;
            if(planAnswer != null && planAnswer.containsKey(A_0003) && planAnswer.get(A_0003) != null){
                answer = (boolean)planAnswer.get(A_0003);
                log.info(A_0003 + " : " + answer);
            }

            if(answer){
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
            boolean isAdjustmentTargetArea = checkAdjustmentTargetArea(StringUtils.defaultString(house.getRoadAddr()));

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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
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
                calculationSellResultResponse = getCalculationBuyResultResponse(calculationSellResultRequest, taxRateCode, dedCode);
            }

            return calculationSellResultResponse;
        }

        // 양도소득세 계산 결과 조회
        private CalculationSellResultResponse getCalculationBuyResultResponse(CalculationSellResultRequest calculationSellResultRequest, String taxRateCode, String dedCode){
            log.info(">>> CalculationBranch getCalculationSellResult - 양도소득세 계산 수행");

            // 양도소득세 계산 결과 세팅
            List<CalculationSellOneResult> calculationSellResultOneList = new ArrayList<>();

            // 양도주택정보
            House house = houseRepository.findByHouseId(calculationSellResultRequest.getHouseId())
                    .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR));

            // 세율정보
            TaxRateInfo taxRateInfo = null;
            if(taxRateCode != null && !taxRateCode.isBlank()){
                log.info("세율정보 조회");
                taxRateInfo = taxRateInfoRepository.findByTaxRateCode(taxRateCode)
                        .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "세율 정보를 가져오는 중 오류가 발생했습니다."));
            }

            // 공제정보
            DeductionInfo deductionInfo = null;
            if(dedCode != null && !dedCode.isBlank()){
                log.info("공제정보 조회");
                deductionInfo = deductionInfoRepository.findByDedCode(dedCode)
                        .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "공제 정보를 가져오는 중 오류가 발생했습니다."));
            }

            long ownHouseCount = getOwnHouseCount();                            // 보유주택 수
            int ownerCount = house.getOwnerCnt();                               // (양도주택)소유자 수
            double userProportion = (double)house.getUserProportion() / 100;    // (양도주택)보유주택비율(소유자1)
            double restProPortion = 1 - userProportion;                         // (양도주택)보유주택비율(소유자2)

            log.info("- 보유주택 수 : " + ownHouseCount);

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
                long progDeductionPrice = 0;    // 누진공제금액
                long totalTaxPrice = 0;         // 총납부세액
                long retentionPeriodDay = 0;    // 보유기간(일)
                long retentionPeriodYear = 0;   // 보유기간(년)

                double sellTaxRate = 0;         // 양도소득세율
                double taxRate1 = 0;            // 세율1
                double taxRate2 = 0;            // 세율2
                double addTaxRate1 = 0;         // 추가세율1
                double addTaxRate2 = 0;         // 추가세율2
                double finalTaxRate1 = 0;       // 최종세율1
                double finalTaxRate2 = 0;       // 최종세율2

                double dedRate = 0;             // 공제율

                long buyPrice = (long)(house.getBuyPrice() * proportion);                               // 취득가액
                LocalDate buyDate = house.getBuyDate();                                                 // 취득일자
                long sellPrice = (long)(calculationSellResultRequest.getSellPrice() * proportion);      // 양도가액
                LocalDate sellDate = calculationSellResultRequest.getSellDate();                        // 양도일자
                long necExpensePrice = calculationSellResultRequest.getNecExpensePrice();               // 필요경비금액
                sellProfitPrice = sellPrice - (buyPrice + necExpensePrice);                             // 양도차익금액(양도가액 - (취득가액 + 필요경비))

                // 양도차익금액이 0보다 작으면 0으로 세팅
                if(sellProfitPrice < 0) sellProfitPrice = 0;

                retentionPeriodDay = ChronoUnit.DAYS.between(buyDate, sellDate);                        // 보유기간(일)
                retentionPeriodYear = ChronoUnit.YEARS.between(buyDate, sellDate);                      // 보유기간(년)

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
                    // 세율이 상수인 경우
                    if(YES.equals(taxRateInfo.getConstYn())){
                        sellTaxRate = Double.parseDouble(StringUtils.defaultString(taxRateInfo.getTaxRate1(), ZERO));
                        sellTaxPrice = (long)(buyPrice * sellTaxRate) - progDeductionPrice;
                    }
                    // 세율이 상수가 아닌 경우(변수)
                    else{
                        // 세율1이 비과세인지 체크(비과세대상양도차익금액 세팅여부를 확인)
                        if(NONE_AND_GENERAL_TAX_RATE.equals(taxRateInfo.getTaxRate1())){
                            // 비과세대상양도차익금액
                            if(taxRateInfo.getBasePrice() != null){
                                nonTaxablePrice = (taxRateInfo.getBasePrice() * sellProfitPrice) / sellPrice;
                            }
                        }

                        // 과세대상양도차익금액
                        taxablePrice = sellProfitPrice - nonTaxablePrice;

                        // 양도차익금액이 0보다 작으면 0으로 세팅
                        if(taxablePrice < 0) taxablePrice = 0;

                        // 공제율 및 장기보유특별공제금액(공제정보가 존재하는 경우에만 계산)
                        if(deductionInfo != null){
                            // 공제율
                            dedRate = calculateDeductionRate(deductionInfo, retentionPeriodYear, calculationSellResultRequest.getStayPeriodYear());

                            // 장기보유특별공제금액(과세대상양도차익금액 x 공제율)
                            longDeductionPrice = (long)(taxablePrice * dedRate);
                        }

                        // 양도소득금액(과세대상양도차익금액 - 장기보유특별공제금액)
                        sellIncomePrice = taxablePrice - longDeductionPrice;
                        if(sellIncomePrice < 0) sellIncomePrice = 0;

                        // 과세표준금액(양도소득금액 - 기본공제금액)
                        taxableStdPrice = sellIncomePrice - BASIC_DEDUCTION_PRICE;
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
                                    taxRate1 = calculateGeneralTaxRate(taxableStdPrice);
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
                                    if(sellTaxPrice < 0) sellTaxPrice = 0;
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
                                if(sellTaxPrice < 0) sellTaxPrice = 0;
                            }
                        }
                    }

                    // 지방소득세액(양도소득세의 10%)
                    localTaxPrice = (long)(sellTaxPrice * LOCAL_TAX_RATE);

                    // 총납부세액(양도소득세 + 지방소득세)
                    totalTaxPrice = sellTaxPrice + localTaxPrice;
                }

                log.info("양도소득세 계산 END");

                log.info("----------------------------------");
                log.info("- 비과세대상양도차익금액 : " + nonTaxablePrice);
                log.info("- 과세대상양도차익금액 : " + taxablePrice);
                log.info("- 장기보유특별공제금액 : " + longDeductionPrice);
                log.info("- 양도소득금액 : " + sellIncomePrice);
                log.info("- 기본공제금액 : " + BASIC_DEDUCTION_PRICE);
                log.info("- 과세표준금액 : " + taxableStdPrice);
                log.info("- 양도소득세율 : " + sellTaxRate);
                log.info("- 누진공제금액 : " + progDeductionPrice);
                log.info("- 양도소득세액 : " + sellTaxPrice);
                log.info("- 지방소득세액 : " + localTaxPrice);
                log.info("- 총납부세액 : " + totalTaxPrice);
                log.info("----------------------------------");

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
                String basicDeductionPriceStr = Long.toString(BASIC_DEDUCTION_PRICE);

                String taxableStdPriceStr = Long.toString(taxableStdPrice);
                String sellTaxRateStr = String.format("%.2f", sellTaxRate*100);
                String progDeductionPriceStr = Long.toString(progDeductionPrice);
                String sellTaxPriceStr = Long.toString(sellTaxPrice);
                String localTaxPriceStr = Long.toString(localTaxPrice);

                String totalTaxPriceStr = Long.toString(totalTaxPrice);

                calculationSellResultOneList.add(
                        CalculationSellOneResult.builder()
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
                                .progDeductionPrice(progDeductionPriceStr)
                                .sellTaxPrice(sellTaxPriceStr)
                                .localTaxPrice(localTaxPriceStr)
                                .totalTaxPrice(totalTaxPriceStr)
                                .build());
            }

            return CalculationSellResultResponse.builder()
                    .listCnt(ownerCount)
                    .list(calculationSellResultOneList)
                    .build();
        }

        private CalculationSellResultResponse getCalculationBuyResultResponseTest(){
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

        // 보유주택 수 가져오기
        private long getOwnHouseCount(){
            log.info(">>> CalculationBranch getOwnHouseCount - 보유주택 수 가져오기");

            User findUser = userUtil.findCurrentUser(); // 호출 사용자 조회
            return houseRepository.countByUserId(findUser.getId());
        }

        // 신규주택 가져오기
        private House getNewOwnHouse(){
            List<House> houseList = houseRepository.findByUserId(userUtil.findCurrentUser().getId()).orElse(null);
            House newOwnHouse = null;
            LocalDate newBuyDate = null;

            if(houseList != null && !houseList.isEmpty()){
                for(House house : houseList){
                    LocalDate buyDate = house.getBuyDate();
                    if(buyDate == null){
                        newBuyDate = buyDate;
                        newOwnHouse = house;
                    }else{
                        if(buyDate.isAfter(newBuyDate)){
                            newBuyDate = buyDate;
                            newOwnHouse = house;
                        }
                    }
                }
            }

            return newOwnHouse;
        }

        // 종전주택 가져오기
        private House getLastOwnHouse(){
            List<House> houseList = houseRepository.findByUserId(userUtil.findCurrentUser().getId()).orElse(null);
            House lastOwnHouse = null;
            LocalDate lastBuyDate = null;

            if(houseList != null && !houseList.isEmpty()){
                houseList.remove(getNewOwnHouse()); // 신규주택을 목록에서 제거(종전주택을 구하기 위함)
                
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

        // 공제율 계산(TODO. 거주기간 체크)
        private double calculateDeductionRate(DeductionInfo deductionInfo, Long rPeriod, Long sPeriod){
            double dedRate = 0;

            String unit = StringUtils.defaultString(deductionInfo.getUnit());   // 단위
            double unitDedRate = deductionInfo.getUnitDedRate();                // 단위공제율
            int limitYear = deductionInfo.getLimitYear();                       // 한도연수
            double limitDedRate = deductionInfo.getLimitDedRate();              // 한도공제율

            long retentionPeriodYear = (rPeriod != null) ? rPeriod : 0;
            long stayPeriodYear = (sPeriod != null) ? sPeriod : 0;

            if(UNIT_1YEAR.equals(unit)){
                dedRate = Math.min(retentionPeriodYear * unitDedRate, limitDedRate);
            }

            return dedRate;
        }

        // 조정대상지역 체크(TODO. 조정대상지역 history까지 체크하여 조정지역여부 체크)
        private boolean checkAdjustmentTargetArea(String address){
            String siGunGu = houseAddressService.separateAddress(address).getSiGunGu();

            // 조정대상지역(용산구, 서초구, 강남구, 송파구)
            return ADJUSTMENT_TARGET_AREA1.equals(siGunGu) || ADJUSTMENT_TARGET_AREA2.equals(siGunGu) || ADJUSTMENT_TARGET_AREA3.equals(siGunGu) || ADJUSTMENT_TARGET_AREA4.equals(siGunGu);
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
                // n년이 된 날 이후
                else if(NOT_WITHIN_YST.equals(dataMethod)){
                    if(inputPeriod > variablePeriod - 1){
                        result = true;
                    }
                }
                // n년이 된 날 이후
                else if(NOT_WITHIN_TMR.equals(dataMethod)){
                    if(inputPeriod > variablePeriod + 1){
                        result = true;
                    }
                }
            }

            return result;
        }
    }
}
