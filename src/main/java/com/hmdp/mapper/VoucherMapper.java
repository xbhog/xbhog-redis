package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author xbhog
 * @since 2023年2月27日
 */
public interface VoucherMapper extends BaseMapper<Voucher> {

    /**
     * @describe: 根据店铺Id查询
     * @param shopId 店铺ID
     */
    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
