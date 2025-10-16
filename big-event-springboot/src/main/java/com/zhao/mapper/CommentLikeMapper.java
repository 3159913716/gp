package com.zhao.mapper;

import com.zhao.pojo.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentLikeMapper {
    
    /**
     * 根据评论ID和用户ID查询点赞记录
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 点赞记录
     */
    CommentLike findByCommentIdAndUserId(@Param("commentId") Integer commentId, @Param("userId") Integer userId);
    
    /**
     * 插入点赞记录
     * @param commentLike 点赞记录
     */
    void insert(CommentLike commentLike);
    
    /**
     * 逻辑删除点赞记录
     * @param commentId 评论ID
     * @param userId 用户ID
     */
    void deleteByCommentIdAndUserId(@Param("commentId") Integer commentId, @Param("userId") Integer userId);
    
    /**
     * 恢复点赞记录（取消删除）
     * @param commentId 评论ID
     * @param userId 用户ID
     */
    void restoreByCommentIdAndUserId(@Param("commentId") Integer commentId, @Param("userId") Integer userId);
    
    /**
     * 更新评论的点赞数量
     * @param commentId 评论ID
     * @param increment 增量（+1或-1）
     */
    void updateCommentLikeCount(@Param("commentId") Integer commentId, @Param("increment") Integer increment);
    
    /**
     * 获取评论的点赞数量
     * @param commentId 评论ID
     * @return 点赞数量
     */
    Integer getCommentLikeCount(@Param("commentId") Integer commentId);
}