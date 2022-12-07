package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private UserMapper userMapper;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("用户手机号格式问题");
        }
        //生成验证码
        String code = RandomUtil.randomString(6);
        //保存到session中
        session.setAttribute("code",code);
        //todo 对接第三方短信
        log.info("======>发送验证码成功");
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机号格式有误");
        }
        if(!StringUtils.isBlank(loginForm.getCode()) || !loginForm.getCode().equals(session.getAttribute("code"))){
            return Result.fail("验证码错误");
        }
        User user = query().select(loginForm.getPhone()).one();
        //用户不存在，创建
        if(Objects.isNull(user)){
            String userName = "user_"+RandomUtil.randomString(6);
            user = createUser(loginForm.getPhone(),userName);
        }
        session.setAttribute("user",user);
        return Result.ok();
    }

    /**
     * 创建用户信息
     * @param phone
     * @param userName
     * @return
     */
    private User createUser(String phone, String userName) {
        User user = new User();
        user.setId(RandomUtil.randomLong());
        user.setPhone(phone);
        user.setNickName(userName);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        save(user);
        log.info("创建用户成功：{}",user);
        return user;
    }
}
