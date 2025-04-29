package com.xuecheng.ucenter.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


/**
 * 账号密码方式认证
 */
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Resource
    private XcUserMapper xcUserMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private CheckCodeClient checkCodeClient;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto){
        // 账号
        String username = authParamsDto.getUsername();

        // 校验验证码
        // 输入的验证码
        String checkCode = authParamsDto.getCheckcode();
        // 验证码对应的key
        String checkCodeKey = authParamsDto.getCheckcodekey();

        if(StringUtils.isBlank(checkCode) || StringUtils.isBlank(checkCodeKey)){
            throw new RuntimeException("请输入验证码");
        }

        // 远程调用验证码服务接口去校验验证码
        Boolean verify = checkCodeClient.verify(checkCodeKey, checkCode);
        if(verify == null || !verify){
            throw new RuntimeException("验证码错误");
        }

        // 账号是否存在
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));

        if(xcUser == null){
            // 账号不存在
            throw new RuntimeException("账号不存在");
        }

        // 验证密码是否正确
        String passwordDb = xcUser.getPassword();
        String passwordForm = authParamsDto.getPassword();
        // 校验密码
        boolean matches = passwordEncoder.matches(passwordForm, passwordDb);
        if(!matches){
            throw new RuntimeException("账号或密码错误");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);

        return xcUserExt;
    }
}
