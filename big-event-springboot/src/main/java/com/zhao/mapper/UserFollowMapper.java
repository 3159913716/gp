package com.zhao.mapper;

import com.zhao.pojo.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserFollowMapper {
    
    /**
     * 根据关注者ID和被关注者ID查询关注记录
     * @param followerId 关注者ID
     * @param followedId 被关注者ID
     * @return 关注记录
     */
    UserFollow findByFollowerIdAndFollowedId(@Param("followerId") Integer followerId, @Param("followedId") Integer followedId);
    
    /**
     * 插入关注记录
     * @param userFollow 关注记录
     */
    void insert(UserFollow userFollow);
    
    /**
     * 逻辑删除关注记录
     * @param followerId 关注者ID
     * @param followedId 被关注者ID
     */
    void deleteByFollowerIdAndFollowedId(@Param("followerId") Integer followerId, @Param("followedId") Integer followedId);
    
    /**
     * 恢复关注记录（取消删除）
     * @param followerId 关注者ID
     * @param followedId 被关注者ID
     */
    void restoreByFollowerIdAndFollowedId(@Param("followerId") Integer followerId, @Param("followedId") Integer followedId);
    
    /**
     * 获取用户关注列表
     * @param followerId 关注者ID
     * @return 关注的用户列表
     */
    List<Map<String, Object>> findFollowingList(@Param("followerId") Integer followerId);
    
    /**
     * 获取用户粉丝列表
     * @param followedId 被关注者ID
     * @return 粉丝列表
     */
    List<Map<String, Object>> findFollowersList(@Param("followedId") Integer followedId);
}