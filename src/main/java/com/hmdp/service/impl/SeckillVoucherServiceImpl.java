package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;
    @Resource
    private IVoucherOrderService voucherOrderService;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //查询优惠卷
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //判断秒杀是否开始：开始时间，结束时间
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("活动暂未开始，敬请期待！");
        }
        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("活动已结束，请关注下次活动！");
        }
        //判断库存是否充足
        if(voucher.getStock() < 1){
            return Result.fail("库存不足，正在补充!");
        }
        Long userId = UserHolder.getUser().getId();
        //一人一单逻辑
        Integer count = voucherOrderService.query().eq("voucher_id", voucherId).eq("user_id", userId).count();
        if(count > 0){
            return Result.fail("该用户已参加活动。");
        }
        //开始扣减库存(通过乐观锁--->对应数据库中行锁实现)
        boolean success  = seckillVoucherMapper.updateDateByVoucherId(voucher);
        if(!success){
            return Result.fail("库存不足，正在补充!");
        }
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrderService.save(voucherOrder);
        return Result.ok(orderId);
    }
}
