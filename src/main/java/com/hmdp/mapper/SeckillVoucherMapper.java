package com.hmdp.mapper;

import com.hmdp.entity.SeckillVoucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 Mapper 接口
 * </p>
 *
 * @author xbhog
 * @since 2023年2月27日
 */
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {
    /**
     * @describe: 实现通过秒杀ID更新日期
     * @param voucherId 秒杀ID
     * @return ture&false
     */
    boolean updateDateByVoucherId(@Param("voucherId") Long voucherId);


    /**
     * @describe: 通过voucherId和库存更新日期
     * @param voucherId 秒杀ID
     * @param stock 库存ID
     */
    boolean updateDateByVoucherIdAndStock(@Param("voucherId") Long voucherId,@Param("stock") Integer stock);
}
