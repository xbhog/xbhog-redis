package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

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
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;
    @Resource
    private IVoucherOrderService voucherOrderService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result seckillVoucher(Long voucherId) {
        //查询优惠卷库存信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        log.info("查询秒杀优惠卷：{}",voucher);
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
        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:"+userId,stringRedisTemplate);
        boolean tryLock = simpleRedisLock.tryLock(1200L);
        if(!tryLock){
            return Result.fail("一人一单规则");
        }
        try {
            //获取原始事务代理对象
            IVoucherOrderService iVoucherOrderService = (IVoucherOrderService) AopContext.currentProxy();
            return iVoucherOrderService.createVoucherOrder(voucherId);
        } catch (IllegalStateException e) {
            log.info("一人一单执行失败：{}",e.getMessage());
        } finally {
            simpleRedisLock.unLock();
        }
        return Result.fail("一人一单流程执行异常");
    }
    @Override
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        log.info("开始进行用户秒杀活动：{}",userId);
        //一人一单逻辑
        Integer count = voucherOrderService.query().eq("voucher_id", voucherId).eq("user_id", userId).count();
        if(count > 0){
            return Result.fail("该用户已参加活动。");
        }
        //开始扣减库存(通过乐观锁--->对应数据库中行锁实现)
        boolean success  = seckillVoucherMapper.updateDateByVoucherId(voucherId);
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
