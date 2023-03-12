package com.hmdp.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    /**
     * 保存用户点赞数据的key
     */
    public static final String MAP_KEY_USER_LIKED = "MAP_USER_LIKED";
    /**
     * 保存blog被点赞数量的key
     */
    public static final String MAP_KEY_BLOG_LIKED_COUNT  = "MAP_BLOG_LIKED_COUNT";

    public static final String BLOG_LIKED_KEY = "blogId:likedId:";
    public static final String USER_LIKED_KEY = "userId:";
    public static final Long UNLIKED_KEY = 0L;
    public static final Long LIKED_KEY = 1L;
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
}
