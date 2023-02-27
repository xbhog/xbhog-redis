package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author xbhog
 * @since 2023年2月27日
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {
    /**
     * 事务代理调用方法
     * @param voucherOrder 订单类
     */
    void createVoucherOrder(VoucherOrder voucherOrder);

    /**
     * 秒杀订单
     * @param voucherId 订单Id
     */
    Result seckillVoucher(Long voucherId);
}
