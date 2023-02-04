package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author xbhog
 * @describe:全局唯一ID
 * @date 2023/1/30
 */
@Component
public class RedisIdWorker {
    //开始时间戳
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    //序列号的位数
    private static final short COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 使用long来接收，8字节-64比特位实现：分为符号位-时间戳-序列号
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix){
        //生成时间戳
        LocalDateTime dateTime = LocalDateTime.now();
        //秒数设置时区
        long nowSecond = dateTime.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        //生成序列号
        //获取当日日期，精确到天
        String date = dateTime.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //自增长上限2^64
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        //左移32位(时间戳)，右32补零(序列号)，然后进行与运算获得64位
        return timestamp << COUNT_BITS | count;
    }

    public static void main(String[] args) {
        LocalDateTime time = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        long l = time.toEpochSecond(ZoneOffset.UTC);
        System.out.println(l);
    }
}
