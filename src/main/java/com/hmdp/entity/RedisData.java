package com.hmdp.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author xbhog
 * @describe:
 * @date 2023/1/15
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
