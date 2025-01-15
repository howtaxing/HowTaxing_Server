package com.xmonster.howtaxing.service.refund;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.dto.common.ApiResponse;
import com.xmonster.howtaxing.dto.refund.RefundConsultingApplyInfoRequest;
import com.xmonster.howtaxing.model.refund.RefundConsultingApplyInfo;
import com.xmonster.howtaxing.repository.refund.RefundConsultingApplyInfoRepository;
import com.xmonster.howtaxing.type.ErrorCode;
import com.xmonster.howtaxing.utils.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import static com.xmonster.howtaxing.constant.CommonConstant.*;

import javax.transaction.Transactional;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RefundService {
    private final RefundConsultingApplyInfoRepository refundConsultingApplyInfoRepository;

    private final UserUtil userUtil;

    // (양도소득세)환급상담신청
    public Object applyRefundConsulting(RefundConsultingApplyInfoRequest refundConsultingApplyInfoRequest) throws Exception {
        log.info(">> [Service]RefundService applyRefundConsulting - (양도소득세)환급상담신청");

        if(refundConsultingApplyInfoRequest == null){
            throw new CustomException(ErrorCode.REFUND_APPLY_INPUT_ERROR);
        }

        String customerPhone = refundConsultingApplyInfoRequest.getCustomerPhone();
        Boolean isRefundAvailable = refundConsultingApplyInfoRequest.getIsRefundAvailable();

        if(StringUtils.isBlank(customerPhone)){
            throw new CustomException(ErrorCode.REFUND_APPLY_INPUT_ERROR, "고객 전화번호가 입력되지 않았어요.");
        }

        if(isRefundAvailable == null){
            throw new CustomException(ErrorCode.REFUND_APPLY_INPUT_ERROR, "환급대상여부 값이 입력되지 않았어요.");
        }

        // 고객전화번호에 '-'이 있으면 제거
        customerPhone = customerPhone.replace(HYPHEN, EMPTY);

        try{
            refundConsultingApplyInfoRepository.saveAndFlush(
                    RefundConsultingApplyInfo.builder()
                            .userId(userUtil.findCurrentUserId())
                            .customerPhone(customerPhone)
                            .isRefundAvailable(isRefundAvailable)
                            .isConsultingCompleted(false)
                            .build());
        }catch(Exception e){
            throw new CustomException(ErrorCode.REFUND_APPLY_OUTPUT_ERROR, "환급상담신청 정보 DB 저장 중 오류가 발생했어요.");
        }

        return ApiResponse.success(Map.of("result", "양도소득세 환급액 조회 신청이 완료되었어요."));
    }
}
