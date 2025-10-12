package com.zhao.service;

import java.util.Map;

public interface ArticleLikeService {
    
    /**
     * 点赞/取消点赞文章
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 包含点赞状态和总点赞数的Map
     */
    Map<String, Object> toggleLike(Integer articleId, Integer userId);
}