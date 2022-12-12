package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;

import static com.hmdp.utils.SystemConstants.SHOP_CACHE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
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
        stringRedisTemplate.opsForValue().set(SHOP_CACHE_KEY+id,JSONUtil.toJsonStr(shop));
        return Result.ok(shop);
    }
}
