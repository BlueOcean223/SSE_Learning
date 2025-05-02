package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

//自定义异常处理
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // 对自定义异常进行处理
    @ResponseBody
    @ExceptionHandler(XueChengPlusException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(XueChengPlusException e) {
        // 记录异常
        log.error("系统异常{}",e.getErrMessage());
        // 解析异常信息
        String errMessage = e.getErrMessage();
        return new RestErrorResponse(errMessage);
    }

    // 对系统抛出的异常进行处理
    @ResponseBody
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse exception(Exception e) {
        // 记录异常
        log.error("系统异常{}",e.getMessage());

        // 权限不足异常
        if(e.getMessage().equals("不允许访问")){
            return new RestErrorResponse("您没有权限操作此功能");
        }

        // 解析异常信息
        return new RestErrorResponse(CommonError.UNKNOWN_ERROR.getErrMessage());
    }
}
