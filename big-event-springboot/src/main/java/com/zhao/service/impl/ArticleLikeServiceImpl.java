package com.zhao.service.impl;

import com.zhao.mapper.ArticleLikeMapper;
import com.zhao.mapper.ArticleMapper;
import com.zhao.pojo.Article;
import com.zhao.pojo.ArticleLike;
import com.zhao.service.ArticleLikeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ArticleLikeServiceImpl implements ArticleLikeService {
    
    @Autowired
    private ArticleLikeMapper articleLikeMapper;
    
    @Autowired
    private ArticleMapper articleMapper;
    
    private static final String PUBLISHED_STATE = "已发布";
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Integer articleId, Integer userId) {
        // 1. 验证文章是否存在且状态为"已发布"
        Article article = articleMapper.findById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!PUBLISHED_STATE.equals(article.getState())) {
            throw new RuntimeException("只能点赞已发布的文章");
        }
        
        // 2. 查询用户是否已点赞
        ArticleLike existingLike = articleLikeMapper.findByArticleIdAndUserId(articleId, userId);
        
        boolean liked = false;
        
        if (existingLike == null) {
            // 未点赞，创建新的点赞记录
            ArticleLike newLike = new ArticleLike();
            newLike.setArticleId(articleId);
            newLike.setUserId(userId);
            newLike.setCreateTime(LocalDateTime.now());
            newLike.setIsDeleted(0);
            articleLikeMapper.insert(newLike);
            // 增加文章点赞数
            articleMapper.incrementLikeCount(articleId);
            liked = true;
        } else {
            if (existingLike.getIsDeleted() == 0) {
                // 已点赞，取消点赞（逻辑删除）
                articleLikeMapper.deleteByArticleIdAndUserId(articleId, userId);
                // 减少文章点赞数
                articleMapper.decrementLikeCount(articleId);
                liked = false;
            } else {
                // 已取消点赞，恢复点赞
                articleLikeMapper.restoreByArticleIdAndUserId(articleId, userId);
                // 增加文章点赞数
                articleMapper.incrementLikeCount(articleId);
                liked = true;
            }
        }
        
        // 3. 获取最新的点赞数
        Article updatedArticle = articleMapper.findById(articleId);
        Integer likeCount = updatedArticle.getLikeCount() != null ? updatedArticle.getLikeCount() : 0;
        
        // 4. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", likeCount);
        
        return result;
    }
}