# Redis场景实践
教程：bilibi黑马redis从入门到实践
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