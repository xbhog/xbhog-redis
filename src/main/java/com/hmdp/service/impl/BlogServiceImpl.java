package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author xbhog
 * @since 2023年2月27日
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private IBlogService blogService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if (ObjectUtil.isNull(blog)) {
            return Result.fail("笔记不存在");
        }
        queryBlogUser(blog);
        return Result.ok(blog);
    }

    @Override
    public List<Blog> queryHotBlogById(Integer current) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(this::queryBlogUser);
        return records;
    }

    /**
     * 修改点赞信息
     * @param id
     * @return
     */
    @Override
    public Result changeLikeStatus(Long id) {
        //获取登录用户
        String userId = UserHolder.getUser().getId().toString();
        String key = BLOG_LIKED_KEY+id;
        Boolean member = stringRedisTemplate.opsForSet().isMember(key, userId);
        if(BooleanUtil.isFalse(member)){
            //未点赞
            //数据库点赞
            //保存到set集合中
            stringRedisTemplate.opsForSet().add(key,userId);
        }else{
            //点赞
            stringRedisTemplate.opsForSet().remove(key,userId);
        }
        return Result.ok("点赞成功");
    }

    /**
     * 构建key值
     * @param likedId
     * @param blogId
     * @return
     */
    private String getlikedKey(Long likedId, Long blogId) {
        return likedId + "::" + blogId;
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
