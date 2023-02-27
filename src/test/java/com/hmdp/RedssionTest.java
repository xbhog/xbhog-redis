package com.hmdp;

import org.junit.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author xbhog
 * @describe:
 * @date 2023/2/14
 */
@SpringBootTest
public class RedssionTest {
    @Resource
    private RedissonClient redissonClentUtil;
    @Test
    public void remissionDemo() throws InterruptedException {
        //获取锁(可重入)，指定锁的名称
        RLock anyLock = redissonClentUtil.getLock("anyLock");
        //尝试获取锁，参数分别是：获取锁的最大等待时间(期间会重试)，锁自动释放时间，时间单位
        boolean b = anyLock.tryLock(1, 10, TimeUnit.MINUTES);
        if(b){
            System.out.println("加锁成功");
        }else {
            System.out.println("加锁失败");
        }
    }
}
