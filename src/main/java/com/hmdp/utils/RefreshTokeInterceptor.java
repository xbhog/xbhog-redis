package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.SystemConstants.LOGIN_USER_KEY;

/**
 * @author xbhog
 * @describe: 拦截器实现：校验用户登录状态
 * @date 2022/12/7
 */
public class RefreshTokeInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokeInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");
        //如果页面没有登录，则没有token,直接放行给下一个拦截器
        if(StringUtils.isEmpty(token)){
            return true;
        }
        String tokenKey = LOGIN_USER_KEY + token;
        Map<Object, Object> userRedis = stringRedisTemplate.opsForHash().entries(tokenKey);
        if(userRedis.isEmpty()){
            return true;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userRedis, new UserDTO(), false);
        //用户存在，放到threadLocal
        UserHolder.saveUser(userDTO);
        //登录续期
        stringRedisTemplate.expire(tokenKey,30L, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
