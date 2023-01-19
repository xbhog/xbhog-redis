package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;


/**
 * @author xbhog
 * @describe:基于StringRedisTemplate封装一个缓存工具类，满足下列需求：
 * 方法1：将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
 * 方法2：将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓
 * 存击穿问题
 *
 * 方法3：根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
 * 方法4：根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题
 * @date 2023/1/15
 */
@Slf4j
@Component
public class CacheClientUtil {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 将任意Java对象序列化为json并存储在string类型的key中，并且可以设置TTL过期时间
     */
    public void set(String key, Object value, Long time, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,timeUnit);
    }
    /**
     * 将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓用于处理缓存击穿问题
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit timeUnit){
        RedisData redisData = new RedisData();
        //当前时间+传递的时间=逻辑过期时间；传递的时间不一定为秒(需要转换)
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        redisData.setData(value);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData),time,timeUnit);
    }

    /**
     *  根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
     * @param prefix  key前缀
     * @param id 拼接的ID
     * @param type 转换后的类型
     * @param dbFallback 查询数据库的函数
     * @param time 过期时间
     * @param timeUnit 过期单位
     * @return 对应类型
     * @param <R> 返回值
     * @param <ID> 入参ID类型
     */
    public <R,ID> R queryWithPassThrough(String prefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time,TimeUnit timeUnit) {
        //从redis查询对应信息
        String json = stringRedisTemplate.opsForValue().get(prefix + id);
        //命中缓存，返回店铺信息
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        //redis既没有key的缓存,但查出来信息不为null,则为“”
        if (json != null) {
            return null;
        }
        //未命中缓存
        R r = dbFallback.apply(id);
        if(Objects.isNull(r)){
            //将null添加至缓存，过期时间减少
            stringRedisTemplate.opsForValue().set(prefix+id,"",time, timeUnit);
            return null;
        }
        //存在。写入缓存
        this.set(prefix+id,r,time,timeUnit);
        return r;
    }
    /**
     *  根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题
     * @param prefix  key前缀
     * @param id 拼接的ID
     * @param type 转换后的类型
     * @param dbFallback 查询数据库的函数
     * @param time 过期时间
     * @param timeUnit 过期单位
     * @return 对应类型
     * @param <R> 返回值
     * @param <ID> 入参ID类型
     */
    public <R,ID> R queryWithLogicalExpire(String prefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time,TimeUnit timeUnit) {
        String redisKey = prefix+id;
        String json = stringRedisTemplate.opsForValue().get(redisKey);
        //判断是否存在
        if(Objects.isNull(json)){
            return null;
        }
        //命中判断缓存是否过期
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //过期时间>当前时间=缓存未过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //未过期直接放回数据
            return r;
        }
        //缓存过期操作
        //尝试获取互斥锁
        String lockKey = LOCK_SHOP_KEY+id;
        Boolean aBoolean = tryLock(lockKey);
        //获取互斥锁成功，开启新的线程执行缓存重建
        if(aBoolean){
            exectorPool().execute(()->{
                try {
                    //查询数据库
                    R apply = dbFallback.apply(id);
                    //重建缓存
                    this.setWithLogicalExpire(redisKey,r,time,timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
        }
        return r;
    }

    /**
     *互斥锁解决缓存击穿问题
     * @param keyPrefix  key前缀
     * @param id 拼接的ID
     * @param type 转换后的类型
     * @param dbFallback 查询数据库的函数
     * @param time 过期时间
     * @param unit 过期单位
     * @return 对应类型
     * @param <R> 返回值
     * @param <ID> 入参ID类型
     */
    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(shopJson, type);
        }
        // 判断命中的是否是空值
        if (shopJson != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.实现缓存重建
        // 4.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2.判断是否获取成功
            if (!isLock) {
                // 4.3.获取锁失败，休眠并重试
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }
            // 4.4.获取锁成功，根据id查询数据库
            r = dbFallback.apply(id);
            // 5.不存在，返回错误
            if (r == null) {
                // 将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            }
            // 6.存在，写入redis
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 7.释放锁
            unLock(lockKey);
        }
        // 8.返回
        return r;
    }

    /**
     * 线程池的创建
     * @return
     */
    private static ThreadPoolExecutor exectorPool() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5,
                //根据自己的处理器数量+1
                Runtime.getRuntime().availableProcessors()+1,
                2L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(3),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

    /**
     * 缓存加锁
     * @param key
     * @return
     */
    private Boolean tryLock(String key){
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        //防止拆箱发生异常
        return BooleanUtil.isTrue(aBoolean);
    }

    /**
     * 释放锁
     * @param key
     */
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}
