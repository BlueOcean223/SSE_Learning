package com.xuecheng.base.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 通用结果类型
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestResponse<T> {

    // 响应编码，0为正常，-1为错误
    private int code;

    // 响应提示信息
    private String msg;

    // 响应内容
    private T result;

    // 错误消息封装
    public static <T> RestResponse<T> validfail(String msg){
        return new RestResponse<>(-1,msg,null);
    }
    public static <T> RestResponse<T> validfail(String msg,T result){
        return new RestResponse<>(-1,msg,result);
    }

    // 添加正常响应数据（包含响应内容）
    public static <T> RestResponse<T> success(T result){
        return new RestResponse<>(0,"success",result);
    }
    public static <T> RestResponse<T> success(String msg,T result){
        return new RestResponse<>(0,msg,result);
    }

    // 添加正常响应数据（不包含响应内容）
    public static <T> RestResponse<T> success(){
        return new RestResponse<>(0,"success",null);
    }

    public Boolean isSuccessful() {
        return this.code == 0;
    }
}
