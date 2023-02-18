package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author xbhog
 * @describe:
 * @date 2023/2/16
 */
public class SimpleRedisLock implements ILock{

    private final String keyName;
    private final StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.fastUUID().toString(true)+"-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource(("redisUnlock.lua")));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String keyName, StringRedisTemplate stringRedisTemplate) {
        this.keyName = keyName;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(Long timeOutSec) {
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + keyName, threadId + "", timeOutSec, TimeUnit.SECONDS);
        //防止拆箱引发空值异常
        return Boolean.TRUE.equals(isLock);
    }

    @Override
    public void unLock() {
       stringRedisTemplate.execute(UNLOCK_SCRIPT,
               Collections.singletonList(KEY_PREFIX + keyName),
               ID_PREFIX + Thread.currentThread().getId());

    }
    /*@Override
    public void unLock() {
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        //获取当前分布式锁中的value
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + keyName);
        //锁相同则删除
        if(threadId.equals(id)){
            stringRedisTemplate.delete(KEY_PREFIX + keyName);
        }

    }*/
}
