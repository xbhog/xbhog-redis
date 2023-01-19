package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.SystemConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.SystemConstants.PHONE_CODE_KEY;

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
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("用户手机号格式问题");
        }
        //生成验证码
        String code = RandomUtil.randomNumbers(6);
        //保存到redis中
        stringRedisTemplate.opsForValue().set(PHONE_CODE_KEY+phone,code,2L,TimeUnit.MINUTES);
        //todo 对接第三方短信
        log.info("======>发送验证码成功：{}",code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机号格式有误");
        }
        //从redis中获取
        String redisCode = stringRedisTemplate.opsForValue().get(PHONE_CODE_KEY + loginForm.getPhone());
        if(StringUtils.isBlank(loginForm.getCode()) || !loginForm.getCode().equals(redisCode)){
            return Result.fail("验证码错误");
        }
        User user = query().eq("phone",loginForm.getPhone()).one();
        //用户不存在，创建
        if(Objects.isNull(user)){
            String userName = "user_"+RandomUtil.randomString(6);
            user = createUser(loginForm.getPhone(),userName);
        }
        log.debug("=====用户信息：{}",user);
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        //随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //将user对象转为HashMap存储
        Map<String, Object> userBeanToMap = BeanUtil.beanToMap(userDTO, new HashMap<>(), CopyOptions.create().
                setIgnoreNullValue(true).
                setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        //保存到redis中
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userBeanToMap);
        //设置过期时间
        stringRedisTemplate.expire(tokenKey,30L, TimeUnit.MINUTES);
        log.debug("======>执行：{}",token);
        return Result.ok(token);
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
