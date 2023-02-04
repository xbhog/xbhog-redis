package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private RedisIdWorker redisIdWorker;



    /**
     * 增加活动热key
     * @throws Exception
     */
    @Test
    public void productTest() throws Exception {
        shopService.saveShop2Redis(1L,10L);
    }

    /**
     * 测试全局唯一ID生成器
     * @throws InterruptedException
     */
    @Test
    public  void testIdWorker() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(300);
        ExecutorService executorService = Executors.newFixedThreadPool(300);
        Runnable task = ()->{
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id："+id);
            }
            //计数-1
            countDownLatch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            executorService.submit(task);
        }
        //id：148285184708444304    0010 0000 1110 1101 0000 1001 0111 0000 0000 0000 0000 0000 0000 1001 0000
        //id：148285184708444305    0010 0000 1110 1101 0000 1001 0111 0000 0000 0000 0000 0000 0000 1001 0001
        //等待子线程结束
        countDownLatch.await();
        long endTime = System.currentTimeMillis();
        System.out.println("time= "+(endTime-begin));
    }
}
