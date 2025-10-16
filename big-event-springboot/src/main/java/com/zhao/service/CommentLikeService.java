package com.zhao.service;

import java.util.Map;

public interface CommentLikeService {
    
    /**
     * 点赞或取消点赞评论
     * @param commentId 评论ID
     * @return 包含点赞状态和点赞数的Map
     */
    Map<String, Object> likeComment(Integer commentId);
}