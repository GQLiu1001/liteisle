package com.liteisle.common.exception;

import com.liteisle.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
@Slf4j
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {
    @ExceptionHandler(LiteisleException.class)
    public Result<?> handleLiteisleException(LiteisleException e) {
        log.error(e.getMessage(), e);
        return Result.fail(e.getMessage());}

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.error(e.getMessage(), e);
        return Result.fail(e.getMessage());}
}
