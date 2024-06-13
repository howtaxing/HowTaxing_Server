package com.xmonster.howtaxing.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import static com.xmonster.howtaxing.constant.CommonConstant.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ApiResponse<T> {
    // Success : "N", Error : "Y"
    private String errYn;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errCode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errMsg;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errMsgDtl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public static <T> ApiResponse<T> success(T data){
        return ApiResponse.<T>builder()
                .errYn(NO)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String errMsg){
        return ApiResponse.<T>builder()
                .errYn(YES)
                .errCode("9999")
                .errMsg(errMsg)
                .build();
    }

    public static <T> ApiResponse<T> error(String errMsg, String errMsgDtl){
        return ApiResponse.<T>builder()
                .errYn(YES)
                .errCode("9999")
                .errMsg(errMsg)
                .errMsgDtl(errMsgDtl)
                .build();
    }

    public static <T> ApiResponse<T> error(String errMsg, String errMsgDtl, String errCode){
        return ApiResponse.<T>builder()
                .errYn(YES)
                .errCode(errCode)
                .errMsg(errMsg)
                .errMsgDtl(errMsgDtl)
                .build();
    }
}
