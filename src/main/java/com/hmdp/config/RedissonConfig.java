package com.hmdp.config;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xbhog
 * @describe:
 * @date 2023/2/14
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClent(){
        //配置类
        Config config = new Config();
        config.useSingleServer().setAddress("redis://1.15.86.246:6379").setPassword("redisxbhog123");
        //创建RedissonClient对象
        return Redisson.create(config);
    }
}
