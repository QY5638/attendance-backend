package com.quyong.attendance.common.exception;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.common.api.ResultCode;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException exception) {
        return new Result<Object>(exception.getCode(), exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().isEmpty()
                ? ResultCode.BAD_REQUEST.getMessage()
                : exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return new Result<Object>(ResultCode.BAD_REQUEST.getCode(), message, null);
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception exception) {
        return Result.failure(ResultCode.SERVER_ERROR, null);
    }
}
