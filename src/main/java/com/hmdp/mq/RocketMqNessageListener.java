package com.hmdp.mq;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

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

    @SneakyThrows
    @Override
    public void onMessage(MessageExt message) {
        log.info("========>异步消费开始");
        String body = null;
        body = new String(message.getBody(), "UTF-8");
        stringRedisTemplate.delete(body);
        int reconsumeTimes = message.getReconsumeTimes();
        log.info("======>重试次数{}",reconsumeTimes);
        if(reconsumeTimes > 3){
            log.info("消费失败：{}",body);
            return;
        }
        throw new RuntimeException("模拟异常抛出");
    }

}
