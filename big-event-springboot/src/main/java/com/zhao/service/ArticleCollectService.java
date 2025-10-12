package com.zhao.service;

import java.util.Map;

public interface ArticleCollectService {
    
    /**
     * 收藏/取消收藏文章
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 包含收藏状态和总收藏数的Map
     */
    Map<String, Object> toggleCollect(Integer articleId, Integer userId);
}