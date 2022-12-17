package com.hmdp.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.SystemConstants.SHOP_CACHE_KEY;
import static com.hmdp.utils.SystemConstants.SHOP_UPDATE_CACHE_KEY;

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

    @Override
    public Result queryById(Long id) {
        //从redis查询商铺信息
        String shopInfo = stringRedisTemplate.opsForValue().get(SHOP_CACHE_KEY + id);
        //命中缓存，返回店铺信息
        if(StrUtil.isNotBlank(shopInfo)){
            Shop shop = JSONUtil.toBean(shopInfo, Shop.class);
            return Result.ok(shop);
        }
        //未命中缓存
        Shop shop = getById(id);
        if(Objects.isNull(shop)){
            return Result.fail("店铺不存在");
        }
        //对象转字符串
        stringRedisTemplate.opsForValue().set(SHOP_CACHE_KEY+id,JSONUtil.toJsonStr(shop),30L, TimeUnit.MINUTES);
        return Result.ok(shop);
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
        stringRedisTemplate.delete(SHOP_CACHE_KEY + id);
        return Result.ok();
    }
}
