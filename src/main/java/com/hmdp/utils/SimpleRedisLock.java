package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author xbhog
 * @describe:
 * @date 2023/2/16
 */
@Component
public class SimpleRedisLock implements ILock{

    private String keyName;
    private StringRedisTemplate stringRedisTemplate;
    private static final String keyPrefix = "lock:";

    public SimpleRedisLock(String keyName, StringRedisTemplate stringRedisTemplate) {
        this.keyName = keyName;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(Long timeOutSec) {
        long threadId = Thread.currentThread().getId();
        Boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(keyPrefix + keyName, threadId + "", timeOutSec, TimeUnit.SECONDS);
        //防止拆箱引发空值异常
        return Boolean.TRUE.equals(isLock);
    }

    @Override
    public void unLock() {
        stringRedisTemplate.delete(keyPrefix + keyName);
    }
}
