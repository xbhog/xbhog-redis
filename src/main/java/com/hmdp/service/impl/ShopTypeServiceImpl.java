package com.hmdp.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopService;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;
import java.util.Objects;

import static com.hmdp.utils.SystemConstants.DEFAULT_PAGE_SIZE;
import static com.hmdp.utils.SystemConstants.SHOP_TYPE_CACHE_KEY;

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
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IShopService shopService;
    @Override
    public Result queryTypeById(Integer typeId, Integer current) {
        String shopTypeStr = stringRedisTemplate.opsForValue().get(SHOP_TYPE_CACHE_KEY + typeId);
        log.info("======>查看redis:{}",shopTypeStr);
        if(StrUtil.isNotBlank(shopTypeStr)){
            List<Shop> shops = JSONUtil.toList(shopTypeStr, Shop.class);
            return Result.ok(shops);
        }
        Page<Shop> page = shopService.query()
                .eq("type_id", typeId)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        List<Shop> shopRecords = page.getRecords();
        stringRedisTemplate.opsForValue().set(SHOP_TYPE_CACHE_KEY+typeId,JSONUtil.toJsonStr(shopRecords));
        log.info("======>商户类型加入缓存：{}",JSONUtil.toJsonStr(shopRecords));
        return Result.ok(shopRecords);
    }
}
