package com.xmonster.howtaxing.service.question;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.calculation.CalculationBuyResultRequest;
import com.xmonster.howtaxing.dto.calculation.CalculationSellResultRequest;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.house.HouseAddressDto;
import com.xmonster.howtaxing.dto.question.AdditionalQuestionRequest;
import com.xmonster.howtaxing.dto.question.AdditionalQuestionResponse;
import com.xmonster.howtaxing.dto.question.AdditionalQuestionResponse.AnswerSelectListResponse;
import com.xmonster.howtaxing.model.AdjustmentTargetAreaInfo;
import com.xmonster.howtaxing.model.CalculationProcess;
import com.xmonster.howtaxing.model.House;
import com.xmonster.howtaxing.model.User;
import com.xmonster.howtaxing.repository.adjustment_target_area.AdjustmentTargetAreaRepository;
import com.xmonster.howtaxing.repository.calculation.CalculationProcessRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class QuestionService {
    private final HouseAddressService houseAddressService;

    private final HouseRepository houseRepository;
    private final CalculationProcessRepository calculationProcessRepository;
    private final AdjustmentTargetAreaRepository adjustmentTargetAreaRepository;

    private final UserUtil userUtil;
    private final HouseUtil houseUtil;
    
    // 추가질의항목 조회
    public Object getAdditionalQuestion(AdditionalQuestionRequest additionalQuestionRequest) {
        log.info(">> [Service]QuestionService getAdditionalQuestion - 추가질의항목 조회");

        // 보유주택목록
        List<House> userHouseList = houseUtil.findOwnHouseList();

        validationCheckForGetAdditionalQuestion(additionalQuestionRequest);

        log.info("추가질의항목 조회 요청 : " + additionalQuestionRequest.toString());

        // REQUEST
        String calcType = additionalQuestionRequest.getCalcType();
        String questionId = StringUtils.defaultString(additionalQuestionRequest.getQuestionId());
        String answerValue = StringUtils.defaultString(additionalQuestionRequest.getAnswerValue());

        Long sellHouseId = additionalQuestionRequest.getSellHouseId();
        LocalDate sellDate = additionalQuestionRequest.getSellDate();
        Long sellPrice = additionalQuestionRequest.getSellPrice();

        Long ownHouseCnt = additionalQuestionRequest.getOwnHouseCnt();
        LocalDate buyDate = additionalQuestionRequest.getBuyDate();
        String jibunAddr = additionalQuestionRequest.getJibunAddr();

        String questionParamData = EMPTY;

        // RESPONSE
        boolean hasNextQuestion = false;
        String nextQuestionId = null;
        String nextQuestionContent = null;
        boolean isNeedAnswer = false;
        String answerType = null;
        List<AnswerSelectListResponse> answerSelectList = null;

        if(ownHouseCnt == null){
            ownHouseCnt = houseUtil.countOwnHouse();
        }

        List<CalculationProcess> calculationProcessList = null;
        String variableData = EMPTY;
        long variablePrice = 0;
        LocalDate variableDate = null;

        // 계산유형 '취득세'인 경우
        if(CALC_TYPE_BUY.equals(calcType)){
            // QuestionID 없이 추가질의가 오는 경우(최초 추가질의)
            if(EMPTY.equals(questionId)){
                log.info("QuestionID 없이 추가질의가 오는 경우(최초 추가질의)");
                // 취득주택 포함 2주택인 경우(1주택 보유중인 경우)
                if(ownHouseCnt == 1){
                    log.info("[getAdditionalQuestion-condition]취득주택 포함 2주택인 경우(1주택 보유중인 경우)");
                    nextQuestionId = Q_0007;
                }
            }
            // QuestionID를 가지고 추가질의가 오는 경우
            else{
                log.info("[getAdditionalQuestion-condition]QuestionID를 가지고 추가질의가 오는 경우 : " + questionId);
                if(Q_0007.equals(questionId)){
                    if(ANSWER_VALUE_01.equals(answerValue)){
                        calculationProcessList = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_BUY, "016")
                                .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "취득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

                        variableData = StringUtils.defaultString(calculationProcessList.get(0).getVariableData());
                        if(variableData.length() == 8){
                            variableDate = LocalDate.parse(variableData, DateTimeFormatter.ofPattern("yyyyMMdd"));
                        }

                        nextQuestionId = Q_0009;
                        questionParamData = (variableDate != null) ? variableDate.toString() : variableData;
                    }else if(ANSWER_VALUE_02.equals(answerValue)){
                        // 신규주택 취득일 기준 조정대상지역에 해당하는 경우
                        if(checkAdjustmentTargetArea(jibunAddr, buyDate)){
                            log.info("[getAdditionalQuestion-condition]신규주택 취득일 기준 조정대상지역 기간에 해당하는 경우");
                            nextQuestionId = Q_0013;
                        }
                    }else{
                        throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "응답값의 범위를 벗어났습니다.");
                    }
                }else if(Q_0009.equals(questionId)){
                    if(ANSWER_VALUE_01.equals(answerValue)){
                        nextQuestionId = Q_0012;
                    }else if(ANSWER_VALUE_02.equals(answerValue)){
                        log.info("다음 추가질의항목 없음, questionId : " + questionId);
                    }else{
                        throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "응답값의 범위를 벗어났습니다.");
                    }
                }else if(Q_0012.equals(questionId)){
                    log.info("다음 추가질의항목 없음, questionId : " + questionId);
                }else if(Q_0013.equals(questionId)){
                    log.info("다음 추가질의항목 없음, questionId : " + questionId);
                }else{
                    throw new CustomException(ErrorCode.QUESTION_OUTPUT_NOT_FOUND, "추가질의항목 조회를 위한 질의ID가 올바르지 않습니다.");
                }
            }
        }
        // 계산유형 '양도소득세'인 경우
        else if(CALC_TYPE_SELL.equals(calcType)){
            if(sellHouseId == null) throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "추가질의항목 조회를 위한 양도주택ID가 입력되지 않았습니다.(양도소득세 추가질의항목 조회 시 필요)");
            if(sellDate == null) throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "추가질의항목 조회를 위한 양도일자가 입력되지 않았습니다.(양도소득세 추가질의항목 조회 시 필요)");
            if(sellPrice == null) throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "추가질의항목 조회를 위한 양도금액이 입력되지 않았습니다.(양도소득세 추가질의항목 조회 시 필요)");

            // 양도주택
            House sellHouse = houseUtil.findSelectedHouse(sellHouseId);

            // QuestionID 없이 추가질의가 오는 경우(최초 추가질의)
            if(EMPTY.equals(questionId)){
                log.info("QuestionID 없이 추가질의가 오는 경우(최초 추가질의)");
                // 1주택 : 취득 계약일 시점 보유주택여부 질의, 실거주기간 질의
                if(ownHouseCnt == 1){
                    log.info("[getAdditionalQuestion-condition]1주택 : 취득 계약일 시점 보유주택여부 질의, 실거주기간 질의");
                    calculationProcessList = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "008")
                            .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

                    variableData = StringUtils.defaultString(calculationProcessList.get(0).getVariableData());

                    if(!EMPTY.equals(variableData) && variableData.length() == 8){
                        // 특정일자(2017.08.03)
                        LocalDate specificDate = LocalDate.parse(variableData, DateTimeFormatter.ofPattern("yyyyMMdd"));

                        // 양도주택 취득일이 특정일자(2017.08.03) 이후인 경우
                        if(sellHouse.getBuyDate().isAfter(specificDate)){
                            log.info("[getAdditionalQuestion-condition]양도주택 취득일이 특정일자(" + specificDate + ") 이후인 경우");
                            // 양도주택 계약일이 특정일자(2017.08.03) 이후인 경우
                            if(sellHouse.getContractDate().isAfter(specificDate)){
                                log.info("[getAdditionalQuestion-condition]양도주택 계약일이 특정일자(" + specificDate + ") 이후인 경우");
                                // 양도주택 취득일 기준 조정대상지역 기간에 해당하는 경우
                                if(checkAdjustmentTargetArea(sellHouse.getJibunAddr(), sellHouse.getBuyDate())){
                                    log.info("[getAdditionalQuestion-condition]양도주택 취득일 기준 조정대상지역 기간에 해당하는 경우");
                                    // 양도주택 계약일 기준 조정대상지역 기간에 해당하는 경우
                                    if(checkAdjustmentTargetArea(sellHouse.getJibunAddr(), sellHouse.getContractDate())){
                                        log.info("[getAdditionalQuestion-condition]양도주택 계약일 기준 조정대상지역 기간에 해당하는 경우");
                                        nextQuestionId = Q_0004;
                                    }else{
                                        log.info("[getAdditionalQuestion-condition]양도주택 계약일 기준 조정대상지역 기간에 해당하지 않는 경우");
                                        nextQuestionId = Q_0008;
                                    }
                                }else{
                                    // 취득일로부터 2년이 된날 다음날 이후 양도하는 경우
                                    if(sellDate.isAfter(sellHouse.getBuyDate().plusYears(2).plusDays(1))){
                                        log.info("[getAdditionalQuestion-condition]취득일로부터 2년이 된날 다음날 이후 양도하는 경우");
                                        calculationProcessList = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "015")
                                                .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

                                        variableData = StringUtils.defaultString(calculationProcessList.get(0).getVariableData(), ZERO);
                                        variablePrice = Long.parseLong(variableData);

                                        // 양도가액 12억 초과
                                        if(sellPrice > variablePrice){
                                            log.info("[getAdditionalQuestion-condition]양도가액 12억 초과");
                                            // 취득일로부터 3년이 된날 다음날 이후 양도하는 경우
                                            if(sellDate.isAfter(sellHouse.getBuyDate().plusYears(3).plusDays(1))) {
                                                log.info("[getAdditionalQuestion-condition]취득일로부터 3년이 된날 다음날 이후 양도하는 경우");
                                                nextQuestionId = Q_0004;
                                            }
                                        }
                                    }
                                }
                            }
                            // 양도주택 계약일이 특정일자(2017.08.03) 이전인 경우
                            else{
                                log.info("[getAdditionalQuestion-condition]양도주택 계약일이 특정일자(" + specificDate + ") 이전인 경우");
                                nextQuestionId = Q_0008;
                                questionParamData = sellHouse.getContractDate().toString();
                            }
                        }
                        // 양도주택 취득일이 특정일자(2017.08.03) 이전인 경우
                        else{
                            log.info("[getAdditionalQuestion-condition]양도주택 취득일이 특정일자(" + specificDate + ") 이전인 경우");
                            calculationProcessList.clear();
                            calculationProcessList = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "015")
                                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

                            variableData = StringUtils.defaultString(calculationProcessList.get(0).getVariableData(), ZERO);
                            variablePrice = Long.parseLong(variableData);

                            // 양도가액 12억 초과
                            if(sellPrice > variablePrice){
                                log.info("[getAdditionalQuestion-condition]양도가액 12억 초과");
                                // 취득일로부터 3년이 된날 다음날 이후 양도하는 경우
                                if(sellDate.isAfter(sellHouse.getBuyDate().plusYears(3).plusDays(1))){
                                    log.info("[getAdditionalQuestion-condition]취득일로부터 3년이 된날 다음날 이후 양도하는 경우");
                                    nextQuestionId = Q_0004;
                                }
                            }
                        }
                    }
                }
                // 2주택 : 종전주택 양도여부 및 신규주택 거주예정기간 질의
                else if(ownHouseCnt == 2){
                    log.info("[getAdditionalQuestion-condition]2주택 : 종전주택 양도여부 및 신규주택 거주예정기간 질의");
                    if(userHouseList != null && userHouseList.size() == 2){
                        House oldHouse = getOldOrNewHouse(userHouseList, false);    // 종전주택
                        House newHouse = getOldOrNewHouse(userHouseList, true);     // 신규주택

                        if(oldHouse != null && newHouse != null){
                            // 두 주택의 취득일이 같지 경우
                            if(oldHouse.getBuyDate() != newHouse.getBuyDate()){
                                log.info("[getAdditionalQuestion-condition]두 주택의 취득일이 같지 경우");
                                // 양도주택이 종전주택인 경우
                                if(sellHouseId.equals(oldHouse.getHouseId())){
                                    log.info("[getAdditionalQuestion-condition]양도주택이 종전주택인 경우");
                                    // 신규주택이 '분양권' 또는 '입주권'인 경우
                                    if(THREE.equals(newHouse.getHouseType()) || FIVE.equals(newHouse.getHouseType()) || newHouse.getIsMoveInRight()){
                                        log.info("[getAdditionalQuestion-condition]신규주택이 '분양권' 또는 '입주권'인 경우");
                                        // 종전주택 취득일로부터 1년이 된 날 다음날 이후 신규주택을 취득한 경우
                                        if(newHouse.getBuyDate().isAfter(oldHouse.getBuyDate().plusYears(1).plusDays(1))){
                                            log.info("[getAdditionalQuestion-condition]종전주택 취득일로부터 1년이 된 날 다음날 이후 신규주택을 취득한 경우");
                                            // 종전주택을 취득일로부터 2년이 된 날 이후 양도하는 경우
                                            if(sellDate.isAfter(oldHouse.getBuyDate().plusYears(2))){
                                                log.info("[getAdditionalQuestion-condition]종전주택을 취득일로부터 2년이 된 날 이후 양도하는 경우");
                                                // 신규주택 취득일로부터 3년이 된 날 다음날 이내에 종전주택 양도하는 경우
                                                if(sellDate.isBefore(newHouse.getBuyDate().plusYears(3).plusDays(1))){
                                                    log.info("[getAdditionalQuestion-condition]신규주택 취득일로부터 3년이 된 날 다음날 이내에 종전주택 양도하는 경우");
                                                    log.info("[getAdditionalQuestion-condition]1주택 로직으로 이동");
                                                    // 1주택 로직으로 이동
                                                    return getAdditionalQuestion(
                                                            AdditionalQuestionRequest.builder()
                                                                    .calcType(CALC_TYPE_SELL)
                                                                    .sellHouseId(sellHouseId)
                                                                    .sellDate(sellDate)
                                                                    .sellPrice(sellPrice)
                                                                    .ownHouseCnt(1L)
                                                                    .build());
                                                }else{
                                                    nextQuestionId = Q_0001;
                                                    questionParamData = newHouse.getBuyDate().plusYears(1).toString();
                                                }
                                            }
                                        }
                                        // 종전주택 취득일로부터 1년이 된 날 다음날 이전에 신규주택을 취득한 경우
                                        else{
                                            log.info("[getAdditionalQuestion-condition]종전주택 취득일로부터 1년이 된 날 다음날 이전에 신규주택을 취득한 경우");
                                            calculationProcessList = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "027")
                                                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

                                            variableData = StringUtils.defaultString(calculationProcessList.get(0).getVariableData());
                                            if(!EMPTY.equals(variableData) && variableData.length() == 8){
                                                // 특정일자(2022.02.15)
                                                LocalDate specificDate = LocalDate.parse(variableData, DateTimeFormatter.ofPattern("yyyyMMdd"));

                                                // 신규주택 취득일이 특정일자(2022.02.15) 이전인 경우
                                                if(newHouse.getBuyDate().isBefore(specificDate)){
                                                    log.info("[getAdditionalQuestion-condition]신규주택 취득일이 특정일자(2022.02.15) 이전인 경우");
                                                    // 종전주택을 취득일로부터 2년이 된 날 이후 양도하는 경우
                                                    if(sellDate.isAfter(oldHouse.getBuyDate().plusYears(2))){
                                                        log.info("[getAdditionalQuestion-condition]종전주택을 취득일로부터 2년이 된 날 이후 양도하는 경우");
                                                        // 신규주택 취득일로부터 3년이 된 날 다음날 이후에 종전주택 양도하는 경우
                                                        if(sellDate.isAfter(newHouse.getBuyDate().plusYears(3).plusDays(1))){
                                                            log.info("[getAdditionalQuestion-condition]신규주택 취득일로부터 3년이 된 날 다음날 이후에 종전주택 양도하는 경우");
                                                            nextQuestionId = Q_0001;
                                                            //questionParamData = newHouse.getBuyDate().plusYears(1).toString();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // 신규주택이 '주택'인 경우
                                    else{
                                        log.info("[getAdditionalQuestion-condition]신규주택이 '주택'인 경우");
                                        // 종전주택 취득일로부터 1년이 된 날 다음날 이후 신규주택을 취득한 경우
                                        if(newHouse.getBuyDate().isAfter(oldHouse.getBuyDate().plusYears(1).plusDays(1))){
                                            log.info("[getAdditionalQuestion-condition]종전주택 취득일로부터 1년이 된 날 다음날 이후 신규주택을 취득한 경우");
                                            // 종전주택을 취득일로부터 2년이 된 날 이후 양도하는 경우
                                            if(sellDate.isAfter(oldHouse.getBuyDate().plusYears(2))){
                                                log.info("[getAdditionalQuestion-condition]종전주택을 취득일로부터 2년이 된 날 이후 양도하는 경우");
                                                // 신규주택 취득일로부터 3년이 된 날 다음날 이내에 종전주택 양도하는 경우
                                                if(sellDate.isBefore(newHouse.getBuyDate().plusYears(3).plusDays(1))){
                                                    log.info("[getAdditionalQuestion-condition]신규주택 취득일로부터 3년이 된 날 다음날 이내에 종전주택 양도하는 경우");
                                                    log.info("[getAdditionalQuestion-condition]1주택 로직으로 이동");
                                                    // 1주택 로직으로 이동
                                                    return getAdditionalQuestion(
                                                            AdditionalQuestionRequest.builder()
                                                                    .calcType(CALC_TYPE_SELL)
                                                                    .sellHouseId(sellHouseId)
                                                                    .sellDate(sellDate)
                                                                    .sellPrice(sellPrice)
                                                                    .ownHouseCnt(1L)
                                                                    .build());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // 그 외(추가질의항목 없음)
                else{
                    log.info("1주택 또는 2주택자가 아니므로 추가질의항목 없음");
                }
            }
            // QuestionID를 가지고 추가질의가 오는 경우
            else{
                log.info("[getAdditionalQuestion-condition]QuestionID를 가지고 추가질의가 오는 경우 : " + questionId);
                if(Q_0001.equals(questionId)){
                    if(ANSWER_VALUE_01.equals(answerValue)){
                        // 1주택 로직으로 이동
                        return getAdditionalQuestion(
                                AdditionalQuestionRequest.builder()
                                        .calcType(CALC_TYPE_SELL)
                                        .sellHouseId(sellHouseId)
                                        .sellDate(sellDate)
                                        .sellPrice(sellPrice)
                                        .ownHouseCnt(1L)
                                        .build());
                    }
                }else if(Q_0004.equals(questionId)){
                    nextQuestionId = Q_0005;
                }else if(Q_0005.equals(questionId)){
                    if(ANSWER_VALUE_01.equals(answerValue)){
                        nextQuestionId = PERIOD_TYPE_DIAL;
                    }else if(ANSWER_VALUE_02.equals(answerValue)){
                        nextQuestionId = PERIOD_TYPE_CERT;
                    }else{
                        throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "응답값의 범위를 벗어났습니다.");
                    }
                }else if(Q_0008.equals(questionId)){
                    if(ANSWER_VALUE_01.equals(answerValue)){
                        nextQuestionId = Q_0004;
                    }else if(ANSWER_VALUE_02.equals(answerValue)){
                        calculationProcessList = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "015")
                                .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

                        variableData = StringUtils.defaultString(calculationProcessList.get(0).getVariableData(), ZERO);
                        variablePrice = Long.parseLong(variableData);

                        // 양도가액 12억 초과
                        if(sellPrice > variablePrice){
                            // 취득일로부터 3년이 된날 다음날 이후 양도하는 경우
                            if(sellDate.isAfter(sellHouse.getBuyDate().plusYears(3).plusDays(1))){
                                nextQuestionId = Q_0004;
                            }
                        }
                    }else{
                        throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "응답값의 범위를 벗어났습니다.");
                    }
                }else if(PERIOD_TYPE_DIAL.equals(questionId) || PERIOD_TYPE_CERT.equals(questionId)){
                    long stayPeriodYear = 0;

                    Map<String, Object> stayPeriodMap = getStayPeriodYearAndMonth(answerValue);

                    if(stayPeriodMap.containsKey(STAY_PERIOD_YEAR)){
                        stayPeriodYear = (long)stayPeriodMap.get(STAY_PERIOD_YEAR);
                    }

                    // 실거주기간이 2년 미만인 경우
                    if(stayPeriodYear < 2){
                        nextQuestionId = Q_0006;
                    }
                }else{
                    log.info("다음 추가질의항목 없음, questionId : " + questionId);
                }
            }
        }
        // 그 외(올바르지 않은 계산유형)
        else{
            throw new CustomException(ErrorCode.QUESTION_OUTPUT_NOT_FOUND, "추가질의항목 조회를 위한 계산유형이 올바르지 않습니다.");
        }

        log.info("[getAdditionalQuestion-condition]추가질의 객체 세팅 : " + nextQuestionId);
        // 추가질의 객체 세팅
        if(Q_0001.equals(nextQuestionId)){
            hasNextQuestion = true;
            nextQuestionContent = "현재 주택을 2채 보유하고 계시네요. 종전주택을 양도하실 예정이신데, 다음의 요건을 모두 만족하시나요?\n\n";
            nextQuestionContent += "1. 입주권 혹은 분양권으로 취득하신 신규주택이 완성된 후 3년 이내에 전입하여 해당 신규주택에 1년 이상 계속 거주\n";
            nextQuestionContent += "2. 신규주택이 완성된 후 종전주택을 3년 이내 양도(혹은 완성일 전 양도)";
            isNeedAnswer = true;
            answerType = ANSWER_TYPE_SELECT;
            answerSelectList = new ArrayList<>();
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_01)
                            .answerContent("네")
                            .build()
            );
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_02)
                            .answerContent("아니오")
                            .build()
            );
        }else if(Q_0004.equals(nextQuestionId)){
            hasNextQuestion = true;
            nextQuestionContent = "정확한 양도소득세 계산을 위해서 실거주 기간이 추가로 필요해요";
        }else if(Q_0005.equals(nextQuestionId)){
            hasNextQuestion = true;
            nextQuestionContent = "실거주 기간을 어떻게 가져올까요?";
            isNeedAnswer = true;
            answerType = ANSWER_TYPE_SELECT;
            answerSelectList = new ArrayList<>();
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_01)
                            .answerContent("직접 입력하기")
                            .build()
            );
            /*answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_02)
                            .answerContent("본인 인증하기")
                            .build()
            );*/
        }else if(Q_0006.equals(nextQuestionId)){
            hasNextQuestion = true;
            nextQuestionContent = "상생임대인에 해당하시나요?";
            isNeedAnswer = true;
            answerType = ANSWER_TYPE_SELECT;
            answerSelectList = new ArrayList<>();
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_01)
                            .answerContent("네")
                            .build()
            );
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_02)
                            .answerContent("아니오")
                            .build()
            );
        }else if(Q_0007.equals(nextQuestionId)){
            hasNextQuestion = true;
            nextQuestionContent = "신규로 취득하는 주택을 포함하여 2주택이시네요. 종전주택(기존에 보유하고 있던 주택)이 혹시 분양권 또는 입주권 이신가요?";
            isNeedAnswer = true;
            answerType = ANSWER_TYPE_SELECT;
            answerSelectList = new ArrayList<>();
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_01)
                            .answerContent("분양권 또는 입주권")
                            .build()
            );
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_02)
                            .answerContent("일반주택")
                            .build()
            );
        }else if(Q_0008.equals(nextQuestionId)){
            hasNextQuestion = true;
            nextQuestionContent = "지금 양도하려는 주택의 취득 계약일(" + questionParamData + ") 시점에 보유한 주택이 있었나요?";
            isNeedAnswer = true;
            answerType = ANSWER_TYPE_SELECT;
            answerSelectList = new ArrayList<>();
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_01)
                            .answerContent("네")
                            .build()
            );
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_02)
                            .answerContent("아니오")
                            .build()
            );
        }else if(PERIOD_TYPE_DIAL.equals(nextQuestionId) || PERIOD_TYPE_CERT.equals(nextQuestionId)){
            hasNextQuestion = true;
        }else if(Q_0009.equals(nextQuestionId)){
            hasNextQuestion = true;
            nextQuestionContent = "종전주택(기존에 보유하고 있던 분양권 또는 입주권)을 " + questionParamData + " 이후에 취득하셨나요?";
            isNeedAnswer = true;
            answerType = ANSWER_TYPE_SELECT;
            answerSelectList = new ArrayList<>();
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_01)
                            .answerContent("네")
                            .build()
            );
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_02)
                            .answerContent("아니오")
                            .build()
            );
        }else if(Q_0012.equals(nextQuestionId)){
            hasNextQuestion = true;
            nextQuestionContent = "종전주택(분양권 또는 입주권)이 완공된 후 3년 이내에 종전(신축) 주택 또는 신규 취득 주택을 양도할 예정이신가요?";
            isNeedAnswer = true;
            answerType = ANSWER_TYPE_SELECT;
            answerSelectList = new ArrayList<>();
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_01)
                            .answerContent("네")
                            .build()
            );
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_02)
                            .answerContent("아니오")
                            .build()
            );
        }else if(Q_0013.equals(nextQuestionId)){
            hasNextQuestion = true;
            nextQuestionContent = "종전주택을 신규 취득주택의 취득일 기준으로 3년 이내에 양도할 예정이신가요?";
            isNeedAnswer = true;
            answerType = ANSWER_TYPE_SELECT;
            answerSelectList = new ArrayList<>();
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_01)
                            .answerContent("네")
                            .build()
            );
            answerSelectList.add(
                    AnswerSelectListResponse.builder()
                            .answerValue(ANSWER_VALUE_02)
                            .answerContent("아니오")
                            .build()
            );
        }

        AdditionalQuestionResponse additionalQuestionResponse = AdditionalQuestionResponse.builder()
                .hasNextQuestion(hasNextQuestion)
                .nextQuestionId(nextQuestionId)
                .nextQuestionContent(nextQuestionContent)
                .isNeedAnswer(isNeedAnswer)
                .answerType(answerType)
                .answerSelectList(answerSelectList)
                .build();

        log.info("추가질의항목 조회 응답 : " + additionalQuestionResponse.toString());

        return ApiResponse.success(additionalQuestionResponse);
    }

    // 추가질의항목 요청 데이터 유효성 검증
    private void validationCheckForGetAdditionalQuestion(AdditionalQuestionRequest additionalQuestionRequest){
        if(additionalQuestionRequest == null) throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR);

        String calcType = StringUtils.defaultString(additionalQuestionRequest.getCalcType());

        Long sellHouseId = additionalQuestionRequest.getSellHouseId();
        LocalDate sellDate = additionalQuestionRequest.getSellDate();
        Long sellPrice = additionalQuestionRequest.getSellPrice();

        Long ownHouseCnt = additionalQuestionRequest.getOwnHouseCnt();
        LocalDate buyDate = additionalQuestionRequest.getBuyDate();
        String jibunAddr = additionalQuestionRequest.getJibunAddr();

        if(EMPTY.equals(calcType)){
            throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "추가질의항목 조회를 위한 계샨유형이 입력되지 않았습니다.");
        }else{
            if(!CALC_TYPE_BUY.equals(calcType) && !CALC_TYPE_SELL.equals(calcType)){
                throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "추가질의항목 조회를 위한 계샨유형이 입력 값이 올바르지 않습니다.");
            }
        }

        if(CALC_TYPE_BUY.equals(calcType)){
            if(ownHouseCnt == null){
                throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "취득세 추가질의항목 조회를 위한 보유주택 수가 입력되지 않았습니다.");
            }

            if(buyDate == null){
                throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "취득세 추가질의항목 조회를 위한 신규주택 취득일자가 입력되지 않았습니다.");
            }

            if(jibunAddr == null){
                throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "취득세 추가질의항목 조회를 위한 신규주택 지번주소가 입력되지 않았습니다.");
            }
        }

        if(CALC_TYPE_SELL.equals(calcType)){
            if(sellHouseId == null){
                throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "양도소득세 추가질의항목 조회를 위한 양도주택 ID가 입력되지 않았습니다.");
            }

            if(sellDate == null){
                throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "양도소득세 추가질의항목 조회를 위한 양도주택 양도일자가 입력되지 않았습니다.");
            }

            if(sellPrice == null){
                throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "양도소득세 추가질의항목 조회를 위한 양도주택 양도가액이 입력되지 않았습니다.");
            }
        }
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

    // 조정대상지역 여부 체크
    private boolean checkAdjustmentTargetArea(String address, LocalDate date){
        boolean isAdjustmentTargetArea = false;

        //HouseAddressDto houseAddressDto = houseAddressService.separateAddress(address);
        HouseAddressDto houseAddressDto = houseAddressService.parseAddress(address);

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
                            stayPeriodYearStr = stayPeriodArr[0];
                            stayPeriodMonthStr = stayPeriodArr[1];
                            stayPeriodYearStr = stayPeriodYearStr.replace("년", EMPTY);
                            stayPeriodMonthStr = stayPeriodMonthStr.replace("개월", EMPTY);
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