package com.zhao.service.impl;

import com.zhao.mapper.UserFollowMapper;
import com.zhao.pojo.UserFollow;
import com.zhao.service.UserFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserFollowServiceImpl implements UserFollowService {
    
    @Autowired
    private UserFollowMapper userFollowMapper;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Boolean> toggleFollow(Integer followerId, Integer followedId) {
        // 1. 验证不能关注自己
        if (followerId.equals(followedId)) {
            throw new RuntimeException("不能关注自己");
        }
        
        // 2. 验证被关注用户是否存在
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE id = ?", Integer.class, followedId);
        if (count == null || count == 0) {
            throw new RuntimeException("被关注用户不存在");
        }
        
        // 3. 查询是否已关注
        UserFollow existingFollow = userFollowMapper.findByFollowerIdAndFollowedId(followerId, followedId);
        
        boolean following = false;
        
        if (existingFollow == null) {
            // 4. 未关注，创建新的关注记录
            UserFollow newFollow = new UserFollow();
            newFollow.setFollowerId(followerId);
            newFollow.setFollowedId(followedId);
            newFollow.setCreateTime(LocalDateTime.now());
            newFollow.setIsDeleted(0);
            userFollowMapper.insert(newFollow);
            following = true;
        } else {
            if (existingFollow.getIsDeleted() == 0) {
                // 5. 已关注，取消关注（逻辑删除）
                userFollowMapper.deleteByFollowerIdAndFollowedId(followerId, followedId);
                following = false;
            } else {
                // 6. 已取消关注，恢复关注
                userFollowMapper.restoreByFollowerIdAndFollowedId(followerId, followedId);
                following = true;
            }
        }
        
        // 7. 返回关注状态
        Map<String, Boolean> result = new HashMap<>();
        result.put("following", following);
        return result;
    }
    
    @Override
    public List<Map<String, Object>> getFollowingList(Integer followerId) {
        // 获取用户关注列表
        return userFollowMapper.findFollowingList(followerId);
    }
    
    @Override
    public List<Map<String, Object>> getFollowersList(Integer followedId) {
        // 获取用户粉丝列表
        return userFollowMapper.findFollowersList(followedId);
    }
}