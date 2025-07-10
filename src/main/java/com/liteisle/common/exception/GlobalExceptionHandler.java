package com.liteisle.common.exception;

import com.liteisle.common.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {
    // 专门处理你的业务异常
    @ExceptionHandler(LiteisleException.class)
    public Result<?> handleLianqingException(LiteisleException e) {
        return Result.fail(e.getMessage());}

    // 捕获所有其他运行时异常（作为兜底），防止敏感信息泄露
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        e.printStackTrace(); // 打印堆栈到服务器日志，方便排查
        // 返回一个通用的、安全的错误信息给前端
        return Result.fail(e.getMessage());}
}
