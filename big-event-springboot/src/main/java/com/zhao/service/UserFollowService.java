package com.zhao.service;

import java.util.List;
import java.util.Map;

public interface UserFollowService {
    
    /**
     * 关注/取消关注用户
     * @param followerId 关注者ID
     * @param followedId 被关注者ID
     * @return 包含关注状态的Map
     */
    Map<String, Boolean> toggleFollow(Integer followerId, Integer followedId);
    
    /**
     * 获取用户关注列表
     * @param followerId 关注者ID
     * @return 关注的用户列表
     */
    List<Map<String, Object>> getFollowingList(Integer followerId);
    
    /**
     * 获取用户粉丝列表
     * @param followedId 被关注者ID
     * @return 粉丝列表
     */
    List<Map<String, Object>> getFollowersList(Integer followedId);
}