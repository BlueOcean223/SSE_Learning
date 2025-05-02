package com.xuecheng.ucenter.service;


import com.xuecheng.ucenter.model.po.XcUser;

// 微信扫码接入
public interface WxAuthService {

    /**
     * 微信扫码认证
     * @param code 授权码
     */
    XcUser wxAuth(String code);
}
