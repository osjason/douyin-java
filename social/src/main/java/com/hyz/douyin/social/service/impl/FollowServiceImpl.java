package com.hyz.douyin.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hyz.douyin.common.common.ErrorCode;
import com.hyz.douyin.common.constant.UserConstant;
import com.hyz.douyin.common.exception.BusinessException;
import com.hyz.douyin.common.model.vo.UserVO;
import com.hyz.douyin.common.service.InnerUserService;
import com.hyz.douyin.common.utils.ThrowUtils;
import com.hyz.douyin.social.mapper.FollowMapper;
import com.hyz.douyin.social.model.entity.Follow;
import com.hyz.douyin.social.service.FollowService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hyz.douyin.common.constant.UserConstant.LOGIN_USER_TTL;

/**
 * @author heguande
 * @description 针对表【tb_follow(关注表)】的数据库操作Service实现
 * @createDate 2023-08-23 23:16:42
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow>
        implements FollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @DubboReference
    private InnerUserService innerUserService;

    @Override
    public void relationAction(String token, Long toUserId, Integer actionType) {
        String key = UserConstant.USER_LOGIN_STATE + token;
        // Token 判断
        String idStr = stringRedisTemplate.opsForValue().get(key);
        ThrowUtils.throwIf(StringUtils.isBlank(idStr), ErrorCode.SOCIAL_OPERATION_ERROR, "您未登录");
        Long userId = Long.valueOf(idStr);

        // 自己与自己不允许关注
        if (userId.equals(toUserId)) {
            throw new BusinessException(ErrorCode.SOCIAL_OPERATION_ERROR, "不能自己关注自己哦");
        }

        // 用户 TTL 更新
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 先判断是否存在这条数据，然后根据 actionType 来进行选则
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, toUserId);
        Follow follow = this.getOne(wrapper);
        if (actionType == 1) {
            // 如果是关注操作
            //
            //  判断关注关系是否存在，关注数+1，被关注数+1这个操作是否成功
            if (follow != null||!innerUserService.relationAction(userId, toUserId, actionType)) {
                throw new BusinessException(ErrorCode.SOCIAL_OPERATION_ERROR, "关注操作失败");
            }
            // 生成对应的 Follow，返回结果
            follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(toUserId);
            this.save(follow);
        } else {
            // 如果是取关操作
            if (follow == null || !innerUserService.relationAction(userId, toUserId, actionType)) {
                throw new BusinessException(ErrorCode.SOCIAL_OPERATION_ERROR, "关注操作失败");
            }
            this.removeById(follow);
        }
    }

    @Override
    public List<UserVO> relationFollowList(String token, Long userId) {
        this.parameterCalibration(token, userId);
        // 返回用户信息列表
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(Follow::getUserId, userId);
        List<Long> userIds = new ArrayList<>();
        for (Follow follow : this.list(wrapper)) {
            userIds.add(follow.getFollowUserId());
        }
        return innerUserService.getFollowUserList(userIds);
    }

    @Override
    public List<UserVO> relationFollowerList(String token, Long userId) {
        this.parameterCalibration(token, userId);
        // 返回用户信息列表
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(Follow::getFollowUserId, userId);
        List<Long> userIds = new ArrayList<>();
        for (Follow follow : this.list(wrapper)) {
            userIds.add(follow.getUserId());
        }
        return innerUserService.getFollowerUserList(userIds, token);
    }


    public void parameterCalibration(String token, Long userId) {
        // 用户 TTL 更新
        String key = UserConstant.USER_LOGIN_STATE + token;
        // id与token判断
        String idStr = stringRedisTemplate.opsForValue().get(key);
        ThrowUtils.throwIf(StringUtils.isBlank(idStr), ErrorCode.SOCIAL_OPERATION_ERROR, "您未登录");
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
        Long id = Long.valueOf(idStr);
        ThrowUtils.throwIf(!id.equals(userId), ErrorCode.SOCIAL_OPERATION_ERROR, "用户无权限");
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
    }
}




