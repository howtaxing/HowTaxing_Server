package com.xmonster.howtaxing.service.question;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.question.AdditionalQuestionRequest;
import com.xmonster.howtaxing.dto.question.AdditionalQuestionResponse;
import com.xmonster.howtaxing.dto.question.AdditionalQuestionResponse.AnswerSelectListResponse;
import com.xmonster.howtaxing.model.CalculationProcess;
import com.xmonster.howtaxing.model.House;
import com.xmonster.howtaxing.model.User;
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

    private final UserUtil userUtil;
    private final HouseUtil houseUtil;
    
    // 추가질의항목 조회
    public Object getAdditionalQuestion(AdditionalQuestionRequest additionalQuestionRequest) {
        log.info(">> [Service]QuestionService getAdditionalQuestion - 추가질의항목 조회");

        // 호출 사용자 조회
        User findUser = userUtil.findCurrentUser();

        List<House> userHouseList = houseRepository.findByUserId(findUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.HOUSE_NOT_FOUND_ERROR));

        validationCheckForGetAdditionalQuestion(additionalQuestionRequest);

        // REQUEST
        String calcType = additionalQuestionRequest.getCalcType();
        String questionId = StringUtils.defaultString(additionalQuestionRequest.getQuestionId());
        String answerValue = StringUtils.defaultString(additionalQuestionRequest.getAnswerValue());
        Long sellHouseId = additionalQuestionRequest.getSellHouseId();
        LocalDate sellDate = additionalQuestionRequest.getSellDate();
        Long sellPrice = additionalQuestionRequest.getSellPrice();
        Long ownHouseCnt = additionalQuestionRequest.getOwnHouseCnt();

        // RESPONSE
        boolean hasNextQuestion = false;
        String nextQuestionId = null;
        String nextQuestionContent = null;
        boolean isNeedAnswer = false;
        String answerType = null;
        List<AnswerSelectListResponse> answerSelectList = null;

        // 1주택 이동 로직에서 재귀호출 오류 발생해서 주석처리함...
        //if(CALC_TYPE_BUY.equals(calcType)){
            if(ownHouseCnt == null){
                ownHouseCnt = getOwnHouseCount(findUser);
            }
        // }else if(CALC_TYPE_SELL.equals(calcType)){
        //     ownHouseCnt = getOwnHouseCount(findUser);
        // }

        List<CalculationProcess> calculationProcessList = null;
        String variableData = EMPTY;
        long variablePrice = 0;

        boolean isNeedAdditionalQuestion = false;

        // 계산유형 '취득세'인 경우
        if(CALC_TYPE_BUY.equals(calcType)){
            if(EMPTY.equals(questionId)){
                // STEP 1. 기존에 1주택을 가지고 있는지 확인
                if(ownHouseCnt == 1){
                    // STEP 2. 추가질의항목 응답
                    hasNextQuestion = true;
                    nextQuestionId = Q_0007;
                    nextQuestionContent = "종전주택 매도 계획에 따라 취득세가 다르게 산출될 수 있어요. 종전주택 매도 계획이 있나요?";
                    isNeedAnswer = true;
                    answerType = ANSWER_TYPE_SELECT;
                    answerSelectList = new ArrayList<>();
                    answerSelectList.add(
                            AnswerSelectListResponse.builder()
                                    .answerValue(ANSWER_VALUE_01)
                                    .answerContent("3년 이내 매도 계획")
                                    .build()
                    );
                    answerSelectList.add(
                            AnswerSelectListResponse.builder()
                                    .answerValue(ANSWER_VALUE_02)
                                    .answerContent("매도 계획 없음")
                                    .build()
                    );
                }
            }else{
                if(Q_0007.equals(questionId)){
                    log.info("다음 추가질의항목 없음, questionId : " + questionId);
                }else{
                    throw new CustomException(ErrorCode.QUESTION_OUTPUT_NOT_FOUND, "추가질의항목 조회를 위한 질의ID가 올바르지 않습니다.");
                }
            }
        }
        // 계산유형 '양도소득세'인 경우
        else if(CALC_TYPE_SELL.equals(calcType)){
            if(sellHouseId == null){
                throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "추가질의항목 조회를 위한 양도주택ID가 입력되지 않았습니다.(양도소득세 추가질의항목 조회 시 필요)");
            }
            
            if(EMPTY.equals(questionId)){
                // 1주택 : 실거주기간 질의
                if(ownHouseCnt == 1){
                    House sellHouse = houseUtil.findSelectedHouse(sellHouseId);

                    if(sellDate == null){
                        throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "추가질의항목 조회를 위한 양도일자가 입력되지 않았습니다.(양도소득세 추가질의항목 조회 시 필요)");
                    }

                    if(sellPrice == null){
                        throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "추가질의항목 조회를 위한 양도금액이 입력되지 않았습니다.(양도소득세 추가질의항목 조회 시 필요)");
                    }

                    calculationProcessList = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "008")
                            .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

                    variableData = StringUtils.defaultString(calculationProcessList.get(0).getVariableData());

                    if(!EMPTY.equals(variableData) && variableData.length() == 8){
                        // 특정일자(2017.08.03)
                        LocalDate specificDate = LocalDate.parse(variableData, DateTimeFormatter.ofPattern("yyyyMMdd"));

                        // 양도주택 취득일이 특정일자(2017.08.03) 이후인 경우
                        if(sellHouse.getBuyDate().isAfter(specificDate)){
                            // 양도주택 계약일이 특정일자(2017.08.03) 이후인 경우
                            if(sellHouse.getContractDate().isAfter(specificDate)){
                                // 취득일 기준 조정대상지역 기간에 해당하는 경우
                                if(checkAdjustmentTargetArea(sellHouse.getRoadAddr(), sellHouse.getBuyDate())){
                                    // Q_0004 -> Q_0005
                                    isNeedAdditionalQuestion = true;
                                }else{
                                    // 취득일로부터 2년이 된날 다음날 이후 매도하는 경우
                                    if(sellDate.isAfter(sellHouse.getBuyDate().plusYears(2).plusDays(1))){
                                        calculationProcessList = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "015")
                                                .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

                                        variableData = StringUtils.defaultString(calculationProcessList.get(0).getVariableData(), ZERO);
                                        variablePrice = Long.parseLong(variableData);

                                        if(sellPrice != null){
                                            // 양도가액 12억 초과
                                            if(sellPrice > variablePrice){
                                                // 취득일로부터 3년이 된날 다음날 이후 매도하는 경우
                                                if(sellDate.isAfter(sellHouse.getBuyDate().plusYears(3).plusDays(1))) {
                                                    // Q_0004 -> Q_0005
                                                    isNeedAdditionalQuestion = true;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }else{
                            calculationProcessList.clear();
                            calculationProcessList = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "015")
                                    .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

                            variableData = StringUtils.defaultString(calculationProcessList.get(0).getVariableData(), ZERO);
                            variablePrice = Long.parseLong(variableData);
                            
                            if(sellPrice != null){
                                // 양도가액 12억 초과
                                if(sellPrice > variablePrice){
                                    // 취득일로부터 3년이 된날 다음날 이후 매도하는 경우
                                    if(sellDate.isAfter(sellHouse.getBuyDate().plusYears(3).plusDays(1))){
                                        // Q_0004 -> Q_0005
                                        isNeedAdditionalQuestion = true;
                                    }
                                }
                            }
                        }
                    }

                    // 추가질의 필요
                    if(isNeedAdditionalQuestion){
                        hasNextQuestion = true;
                        nextQuestionId = Q_0004;
                        nextQuestionContent = "정확한 양도소득세 계산을 위해서 실거주 기간이 추가로 필요해요";
                    }
                }
                // 2주택 : 종전주택 매도여부 및 신규주택 거주예정기간 질의
                else if(ownHouseCnt == 2){
                    if(userHouseList != null && userHouseList.size() == 2){
                        House oldHouse = getOldOrNewHouse(userHouseList, false);    // 종전주택
                        House newHouse = getOldOrNewHouse(userHouseList, true);     // 신규주택

                        if(oldHouse != null && newHouse != null){
                            // 양도주택이 종전주택인 경우
                            if(sellHouseId.equals(oldHouse.getHouseId())){
                                // 신규주택이 '분양권' 또는 '입주권'인 경우
                                if(THREE.equals(newHouse.getHouseType()) || FIVE.equals(newHouse.getHouseType())){
                                    // 종전주택 취득일로부터 1년이 된 날 다음날 이후 신규주택을 취득한 경우
                                    if(newHouse.getBuyDate().isAfter(oldHouse.getBuyDate().plusYears(1).plusDays(1))){
                                        // 종전주택을 취득일로부터 2년이 된 날 이후 매도하는 경우
                                        if(sellDate.isAfter(oldHouse.getBuyDate().plusYears(2))){
                                            // 신규주택 취득일로부터 3년이 된 날 다음날 이내에 종전주택 매도하는 경우
                                            if(sellDate.isBefore(newHouse.getBuyDate().plusYears(3).plusDays(1))){
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
                                                isNeedAdditionalQuestion = true;    // Q_0001
                                            }
                                        }
                                    }else{
                                        calculationProcessList = calculationProcessRepository.findByCalcTypeAndBranchNo(CALC_TYPE_SELL, "027")
                                                .orElseThrow(() -> new CustomException(ErrorCode.CALCULATION_SELL_TAX_FAILED, "양도소득세 프로세스 정보를 가져오는 중 오류가 발생했습니다."));

                                        variableData = StringUtils.defaultString(calculationProcessList.get(0).getVariableData());
                                        if(!EMPTY.equals(variableData) && variableData.length() == 8){
                                            // 특정일자(2022.02.15)
                                            LocalDate specificDate = LocalDate.parse(variableData, DateTimeFormatter.ofPattern("yyyyMMdd"));

                                            // 종전주택 취득일이 특정일자(2022.02.15) 이전인 경우
                                            if(oldHouse.getBuyDate().isBefore(specificDate)){
                                                // 종전주택을 취득일로부터 2년이 된 날 이후 매도하는 경우
                                                if(sellDate.isAfter(oldHouse.getBuyDate().plusYears(2))){
                                                    // 신규주택 취득일로부터 3년이 된 날 다음날 이내에 종전주택 매도하는 경우
                                                    if(sellDate.isBefore(newHouse.getBuyDate().plusYears(3).plusDays(1))){
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
                                                        isNeedAdditionalQuestion = true;    // Q_0001
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                // 신규주택이 '주택'인 경우
                                else{
                                    // 종전주택 취득일로부터 1년이 된 날 다음날 이후 신규주택을 취득한 경우
                                    if(newHouse.getBuyDate().isAfter(oldHouse.getBuyDate().plusYears(1).plusDays(1))){
                                        // 종전주택을 취득일로부터 2년이 된 날 이후 매도하는 경우
                                        if(sellDate.isAfter(oldHouse.getBuyDate().plusYears(2))){
                                            // 신규주택 취득일로부터 3년이 된 날 다음날 이내에 종전주택 매도하는 경우
                                            if(sellDate.isBefore(newHouse.getBuyDate().plusYears(3).plusDays(1))){
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

                                // 추가질의 필요
                                if(isNeedAdditionalQuestion){
                                    hasNextQuestion = true;
                                    nextQuestionId = Q_0001;
                                    nextQuestionContent = "현재 주택을 2채 보유하고 계시네요. 종전주택을 양도하실 예정이신데, 입주권 혹은 분양권으로 취득하신 신규주택에 전입신고 후 1년이 된 날 이후 까지(" + newHouse.getBuyDate().plusYears(1) + " 이후) 계속 거주하실 건가요?";
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
                            }
                        }
                    }
                }
                // 그 외(추가질의항목 없음)
                else{
                    log.info("1주택 또는 2주택자가 아니므로 추가질의항목 없음");
                }
            }else{
                if(Q_0001.equals(questionId)){
                    // 1주택 로직으로 이동
                    return getAdditionalQuestion(
                            AdditionalQuestionRequest.builder()
                                    .calcType(CALC_TYPE_SELL)
                                    .sellHouseId(sellHouseId)
                                    .sellDate(sellDate)
                                    .sellPrice(sellPrice)
                                    .ownHouseCnt(1L)
                                    .build());
                }else if(Q_0004.equals(questionId)){
                    hasNextQuestion = true;
                    nextQuestionId = Q_0005;
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
                    answerSelectList.add(
                            AnswerSelectListResponse.builder()
                                    .answerValue(ANSWER_VALUE_02)
                                    .answerContent("본인 인증하기")
                                    .build()
                    );
                }else if(Q_0005.equals(questionId)){
                    if(ANSWER_VALUE_01.equals(answerValue)){
                        hasNextQuestion = true;
                        nextQuestionId = PERIOD_TYPE_DIAL;
                    }else if(ANSWER_VALUE_02.equals(answerValue)){
                        hasNextQuestion = true;
                        nextQuestionId = PERIOD_TYPE_CERT;
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
                        hasNextQuestion = true;
                        nextQuestionId = Q_0006;
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

        return ApiResponse.success(
                AdditionalQuestionResponse.builder()
                        .hasNextQuestion(hasNextQuestion)
                        .nextQuestionId(nextQuestionId)
                        .nextQuestionContent(nextQuestionContent)
                        .isNeedAnswer(isNeedAnswer)
                        .answerType(answerType)
                        .answerSelectList(answerSelectList)
                        .build());
    }

    // 추가질의항목 요청 데이터 유효성 검증
    private void validationCheckForGetAdditionalQuestion(AdditionalQuestionRequest additionalQuestionRequest){
        if(additionalQuestionRequest == null) throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR);

        String calcType = StringUtils.defaultString(additionalQuestionRequest.getCalcType());

        if(EMPTY.equals(calcType)){
            throw new CustomException(ErrorCode.QUESTION_INPUT_ERROR, "추가질의항목 조회를 위한 계샨유형이 입력되지 않았습니다.");
        }
    }

    // 보유주택 수 조회
    private long getOwnHouseCount(User findUser){
        log.info(">>> QuestionService getOwnHouseCount - 보유주택 수 가져오기");
        return houseRepository.countByUserId(findUser.getId());
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
            }
        }

        return house;
    }

    // 조정대상지역 체크(TODO. 조정대상지역 history까지 체크하여 조정지역여부 체크)
    private boolean checkAdjustmentTargetArea(String address, LocalDate date){
        String siGunGu = houseAddressService.separateAddress(address).getSiGunGu();

        // 조정대상지역(용산구, 서초구, 강남구, 송파구)
        return ADJUSTMENT_TARGET_AREA1.equals(siGunGu) || ADJUSTMENT_TARGET_AREA2.equals(siGunGu) || ADJUSTMENT_TARGET_AREA3.equals(siGunGu) || ADJUSTMENT_TARGET_AREA4.equals(siGunGu);
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