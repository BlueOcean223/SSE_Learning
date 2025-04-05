package com.xuecheng.base.exception;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 与前端约定返回的异常信息模型
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestErrorResponse {
    private String errMessage;
}
