package com.xuecheng.ucenter.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

// 自定义UserDetailsService，因为需要从数据库载入用户，并且需要修改传入的参数以及返回的信息,以及完成不同方式的验证
@Slf4j
@Component
public class UserServiceImpl implements UserDetailsService {


    @Autowired
    private ApplicationContext applicationContext;

    @Resource
    private XcMenuMapper xcMenuMapper;

    // (前端）扩充传入的参数，从原来的username变成AuthParamsDto,json格式
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        // 将传入的json转成AuthParamsDto对象
        AuthParamsDto authParamsDto = null;
        try{
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        }catch (Exception e){
            throw new RuntimeException("请求参数不符合要求");
        }

        // 认证类型
        String authType = authParamsDto.getAuthType();

        // 根据类型从spring容器里取出指定的bean
        String beanName = authType + "_authservice";
        AuthService authService = applicationContext.getBean(beanName, AuthService.class);
        // 调用统一的execute方法完成认证
        XcUserExt xcUserExt = authService.execute(authParamsDto);

        return getUserPrincipal(xcUserExt);
    }

    /**
     * 查询用户信息
     */
    public UserDetails getUserPrincipal(XcUserExt xcUserExt){
        String password = xcUserExt.getPassword();
        // 权限
        String[] authorities = null;
        // 从数据库查询用户权限
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(xcUserExt.getId());
        if(xcMenus != null && !xcMenus.isEmpty()){
            // 使用流获取权限
            authorities = xcMenus.stream().map(XcMenu::getCode).toArray(String[]::new);
        }

        // 置空敏感信息
        xcUserExt.setPassword(null);
        // 将用户信息转JSON
        String userJson = JSON.toJSONString(xcUserExt);
        return User.withUsername(userJson).password(password).authorities(authorities).build();
    }

}
