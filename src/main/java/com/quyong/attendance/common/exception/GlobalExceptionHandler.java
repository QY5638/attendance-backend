package com.quyong.attendance.common.exception;

import com.quyong.attendance.common.api.Result;
import com.quyong.attendance.common.api.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Object> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return new Result<Object>(ResultCode.BAD_REQUEST.getCode(), "请求参数错误", null);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, BindException.class})
    public Result<Object> handleBindingException(Exception exception) {
        return new Result<Object>(ResultCode.BAD_REQUEST.getCode(), "请求参数错误", null);
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception exception) {
        log.error("Unhandled server exception", exception);
        return Result.failure(ResultCode.SERVER_ERROR, null);
    }
}
