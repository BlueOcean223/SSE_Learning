package com.xuecheng.auth.controller;


import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.io.IOException;

@Slf4j
@Controller
public class WxLoginController {

    @Resource
    private WxAuthService wxAuthService;

    // 扫码认证成功后前端重定向到该接口
    @RequestMapping("/wxLogin")
    public String wxLogin(String code, String state) throws IOException{
        log.debug("微信扫码回调，code：{}，state：{}",code,state);
        // 远程调用微信请求令牌，获取用户信息并写入数据库
        XcUser xcUser = wxAuthService.wxAuth(code);

        if(xcUser == null){
            return "redirect:http://www.51xuecheng.cn/error.html";
        }
        String username = xcUser.getUsername();
        return "redirect:http://www.51xuecheng.cn/sign.html?username=" + username + "&authType=wx";
    }
}
