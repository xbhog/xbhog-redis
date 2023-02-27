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
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.*;

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
    private static final DefaultRedisScript<Long> SPIKE_OPT;
    /**
     * 阻塞队列
     */
    private final BlockingQueue<VoucherOrder> orderTakes = new ArrayBlockingQueue<VoucherOrder>(1024*1024);
    /*
    * 创建线程池
    */
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(3,5,
            10, TimeUnit.SECONDS,
            new LinkedBlockingDeque<>());
    static {
        SPIKE_OPT = new DefaultRedisScript<>();
        SPIKE_OPT.setLocation(new ClassPathResource(("SpikeOptimization.lua")));
        SPIKE_OPT.setResultType(Long.class);
    }
    @PostConstruct
    public void init(){
        Runnable VoucherOrderHandler = null;
        EXECUTOR.execute(VoucherOrderHandler);
    }
    private class VoucherOrderHandler implements Runnable{

        @Override
        public void run() {
            while (true){
                 try {
                     VoucherOrder voucherOrder = orderTakes.take();
                     //创建订单
                     handleVoucherOrder(voucherOrder);
                 }catch (Exception e){

                 }
            }
        }

        /**
         * 保存订单
         * @param voucherOrder
         */
        private void handleVoucherOrder(VoucherOrder voucherOrder) {
            //获取用户Id
            Long userId = voucherOrder.getUserId();
            save(voucherOrder);
        }
    }
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
        long orderId = redisIdWorker.nextId("order");
        //执行lua脚本
        Long execute = stringRedisTemplate.execute(SPIKE_OPT, Collections.emptyList(),
                voucherId.toString(), userId.toString());
        assert execute != null;
        int r = execute.intValue();
        if(r != 0){
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        // TODO: 2023/2/25 保存到阻塞队列中
        orderTakes.add(voucherOrder);
        return Result.ok(orderId);
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
