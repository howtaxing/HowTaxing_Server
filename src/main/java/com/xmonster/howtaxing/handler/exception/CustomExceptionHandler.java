package com.xmonster.howtaxing.handler.exception;

import com.xmonster.howtaxing.CustomException;
import com.xmonster.howtaxing.model.ErrorResponseEntity;
import com.xmonster.howtaxing.type.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponseEntity> handleCustomException(CustomException e){

        ErrorCode errorCode = e.getErrorCode();
        String errorMessage = e.getErrorMessage();
        String errorMessageDetail = e.getErrorMessageDetail();

        log.error("ERROR LOG(errorCode : " + errorCode + ", errorMessage : " + errorMessage + ", errorMessageDetail : " + errorMessageDetail + ")");

        if(errorCode != null){
            if(errorMessageDetail != null){
                if(errorMessage != null){
                    return ErrorResponseEntity.toResponseEntity(errorCode, errorMessage, errorMessageDetail);
                }else{
                    return ErrorResponseEntity.toResponseEntity(errorCode, errorMessageDetail);
                }
            }else{
                return ErrorResponseEntity.toResponseEntity(errorCode);
            }
        }else{
            errorCode = ErrorCode.SYSTEM_UNKNOWN_ERROR;
            return ErrorResponseEntity.toResponseEntity(errorCode);
        }
    }
}