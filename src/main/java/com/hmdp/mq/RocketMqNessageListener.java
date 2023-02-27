package com.hmdp.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

import static com.hmdp.service.impl.ShopServiceImpl.TOPIC_SHOP;

/**
 * @author xbhog
 * @describe:
 * @date 2022/12/21
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = TOPIC_SHOP,consumerGroup = "shopRe",
        messageModel = MessageModel.CLUSTERING)
public class RocketMqNessageListener  implements RocketMQListener<MessageExt> {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(MessageExt message) {
        log.info("========>异步消费开始");
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        stringRedisTemplate.delete(body);
        int reconsumeTimes = message.getReconsumeTimes();
        log.info("======>重试次数{}",reconsumeTimes);
        if(3 < reconsumeTimes){
            log.info("消费失败：{}",body);
            return;
        }
        throw new RuntimeException("模拟异常抛出");
    }

}
