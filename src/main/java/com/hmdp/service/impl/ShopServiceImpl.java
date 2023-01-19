package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.RedisData;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.SHOP_CACHE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public static final String TOPIC_SHOP = "shopTopic1";
    @Resource
    private CacheClientUtil cacheClientUtil;


    @Override
    public Result queryById(Long id) {
        //Shop shop = getShop(id);
        //缓存击穿工具类实现
        //内容形式:id1 -> getById(id1) == this:getById()
        //传递值:id,getById返回值Shop
        //Shop shop = cacheClientUtil.queryWithPassThrough(CACHE_SHOP_KEY + id, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //逻辑过期解决缓存击穿
       // Shop shop = cacheClientUtil.queryWithLogicalExpire((CACHE_SHOP_KEY + id, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);

        //互斥锁解决缓存击穿
        Shop shop = cacheClientUtil.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if(Objects.isNull(shop)){
            return Result.fail("店铺信息不存在");
        }
        return Result.ok(shop);
    }

    /**
     * 缓存穿透\击穿的基本操作
     * @param id
     * @return
     */
    private Shop getShop(Long id) {
        //解决缓存穿透
        //Shop shop = queryWithPassThrough(id);
        //互斥锁：解决缓存击穿
        //Shop shop = queryWithMutex(id);
        //利用逻辑过期解决缓存击穿
        Shop shop = queryWithLogicalExpire(id);
        return shop;
    }

    /**
     * 互斥锁：解决缓存击穿
     * @param id
     * @return
     */
    private Shop queryWithMutex(Long id) {
        //从redis查询商铺信息
        String shopInfo = stringRedisTemplate.opsForValue().get(SHOP_CACHE_KEY + id);
        //命中缓存，返回店铺信息
        if(StrUtil.isNotBlank(shopInfo)){
            return JSONUtil.toBean(shopInfo, Shop.class);
        }
        //redis既没有key的缓存,但查出来信息不为null,则为“”
        if(shopInfo != null){
            return null;
        }
        //实现缓存重建
        String lockKey = "lock:shop:"+id;
        Shop shop = null;
        try {
            Boolean aBoolean = tryLock(lockKey);
            if(!aBoolean){
                //加锁失败
                Thread.sleep(50);
                //递归等待
                return queryWithMutex(id);
            }
            //未命中缓存
            shop = getById(id);
            // 5.不存在，返回错误
            if(Objects.isNull(shop)){
                //将null添加至缓存，过期时间减少
                stringRedisTemplate.opsForValue().set(SHOP_CACHE_KEY+id,"",5L, TimeUnit.MINUTES);
                return null;
            }
            //模拟重建的延时
            Thread.sleep(200);
            //对象转字符串
            stringRedisTemplate.opsForValue().set(SHOP_CACHE_KEY+id,JSONUtil.toJsonStr(shop),30L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unLock(lockKey);
        }
        return shop;
    }
    public Shop queryWithLogicalExpire( Long id ) {
        String key = CACHE_SHOP_KEY + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isBlank(json)) {
            // 3.存在，直接返回
            return null;
        }
        // 4.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5.判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())) {
            // 5.1.未过期，直接返回店铺信息
            return shop;
        }
        // 5.2.已过期，需要缓存重建
        // 6.缓存重建
        // 6.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 6.2.判断是否获取锁成功
        if (isLock){
            exectorPool().execute(() -> {

                try {
                    //重建缓存
                    this.saveShop2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        // 6.4.返回过期的商铺信息
        return shop;
    }

    /**
     * 重建缓存
     * @param id 重建ID
     * @param l 过期时间
     */
    public void saveShop2Redis(Long id, long l) {
        //查询店铺信息
        Shop shop = getById(id);
        //封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(l));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    /**
     * 缓存穿透
     * @param id
     * @return
     */
    public Shop queryWithPassThrough(Long id){
        //从redis查询商铺信息
        String shopInfo = stringRedisTemplate.opsForValue().get(SHOP_CACHE_KEY + id);
        //命中缓存，返回店铺信息
        if(StrUtil.isNotBlank(shopInfo)){
            return JSONUtil.toBean(shopInfo, Shop.class);
        }
        //redis既没有key的缓存,但查出来信息不为null,则为“”
        if(shopInfo != null){
            return null;
        }
        //未命中缓存
        Shop shop = getById(id);
        if(Objects.isNull(shop)){
            //将null添加至缓存，过期时间减少
            stringRedisTemplate.opsForValue().set(SHOP_CACHE_KEY+id,"",5L, TimeUnit.MINUTES);
            return null;
        }
        //对象转字符串
        stringRedisTemplate.opsForValue().set(SHOP_CACHE_KEY+id,JSONUtil.toJsonStr(shop),30L, TimeUnit.MINUTES);
        return shop;
    }

    @Override
    @Transactional
    public Result updateShopById(Shop shop) {
        Long id = shop.getId();
        if(ObjectUtil.isNull(id)){
            return Result.fail("====>店铺ID不能为空");
        }
        log.info("====》开始更新数据库");
        //更新数据库
        updateById(shop);
        String shopRedisKey = SHOP_CACHE_KEY + id;
        Message message = new Message(TOPIC_SHOP,"shopRe",shopRedisKey.getBytes());
        //异步发送MQ
        try {
            rocketMQTemplate.getProducer().send(message);
        } catch (Exception e) {
            log.info("=========>发送异步消息失败：{}",e.getMessage());
        }
        //stringRedisTemplate.delete(SHOP_CACHE_KEY + id);
        //int i = 1/0;  验证异常流程后，
        return Result.ok();
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
}
