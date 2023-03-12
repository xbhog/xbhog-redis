# Redis场景实践

深造redis的实现和使用场景
## 【1】基于redis用户登录注册 
分支：origin/20221207-xbhog-sessionRedis
1. 增加发送短信和用户登录注册功能(V)
2. 基于session实现用户登录注册功能 (V) 
3. 基于Redis实现用户登录注册功能  (V)    
## 【2】基于Redis实现缓存及数据库一致
origin/20221212-xbhog-cacheRedis 
1. 基于redis实现店铺缓存(V)
2. 将redis key设置为常量，将商户类型加入缓存(V)
3. 实现缓存与数据库的双写一致(V)
4. 将redis放入中间件，增加RocketMQ重试操作(V)
## 【3】基于Redis实现缓存穿透功能 
origin/20221221-xbhog-cacheBrenkdown
1. 解决缓存击穿功能：
   - 增加空缓存(V)
   - 设置布隆过滤器
## 【4】基于Redis实现缓存雪崩和缓存击穿
20230110-xbhog-Cache_Penetration_Avalance p44
1. 解决缓存雪崩功能(V)
2. 解决缓存击穿功能
   1. 互斥锁实现解决缓存击穿(V)
   2. 基于逻辑过期方式解决缓存击穿(V)
   3. 工具类封装(V)
## 【5】基于Redis实现秒杀问题
20230130-xbhog-redisSpike
1. 自定义全局唯一ID生成器(V)
2. 增加优惠卷下单功能(乐观锁解决秒杀问题)(V)
3. 增加一人一单逻辑(V)
4. 解决一人一单并发问题(悲观锁插入数据，事务失效原因及解决，锁颗粒问题等)(V)
## 【6】基于redis分布式锁实现
20230211-xbhog-redisCloud && 20230214-xbhog-redission
1. 实现分布式锁(V)
2. 解决分布式锁误删操作(V)
3. 解决分布式锁的原子性Lua(V)
## 【7】基于Redission+lua+阻塞队列实现秒杀优化
20230225-xbhog-Second_kill_optimization
1. lua脚本+阻塞队列,更新日期
## 【8】基于Redis的点赞及点赞排行榜设计
20230306-xbhog-blogStructure
1. 增加发布文章功能
2. 通过set集合实现点赞功能
3. 通过zset实现点赞排行榜