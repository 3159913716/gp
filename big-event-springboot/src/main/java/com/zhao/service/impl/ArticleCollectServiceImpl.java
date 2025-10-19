package com.zhao.service.impl;

import com.zhao.mapper.ArticleCollectMapper;
import com.zhao.mapper.ArticleMapper;
import com.zhao.pojo.Article;
import com.zhao.pojo.ArticleCollect;
import com.zhao.service.ArticleCollectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ArticleCollectServiceImpl implements ArticleCollectService {

    @Autowired
    private ArticleCollectMapper articleCollectMapper;

    @Autowired
    private ArticleMapper articleMapper;

    private static final String PUBLISHED_STATE = "已发布";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleCollect(Integer articleId, Integer userId) {
        // 1. 验证文章是否存在且状态为"已发布"
        Article article = articleMapper.findById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!PUBLISHED_STATE.equals(article.getState())) {
            throw new RuntimeException("只能收藏已发布的文章");
        }

        // 2. 查询用户是否有任何状态的收藏记录（包括已删除的）
        ArticleCollect existingCollect = articleCollectMapper.findByArticleIdAndUserIdAnyStatus(articleId, userId);

        boolean collected = false;

        if (existingCollect == null) {
            // 未收藏，创建新的收藏记录
            ArticleCollect newCollect = new ArticleCollect();
            newCollect.setArticleId(articleId);
            newCollect.setUserId(userId);
            newCollect.setCollectTime(LocalDateTime.now());
            newCollect.setIsDeleted(0);
            articleCollectMapper.insert(newCollect);
            // 增加文章收藏数
            articleMapper.incrementCollectCount(articleId);
            collected = true;
        } else {
            if (existingCollect.getIsDeleted() == 0) {
                // 已收藏，取消收藏（逻辑删除）
                articleCollectMapper.deleteByArticleIdAndUserId(articleId, userId);
                // 减少文章收藏数
                articleMapper.decrementCollectCount(articleId);
                collected = false;
            } else {
                // 已取消收藏，恢复收藏
                articleCollectMapper.restoreByArticleIdAndUserId(articleId, userId);
                // 增加文章收藏数
                articleMapper.incrementCollectCount(articleId);
                collected = true;
            }
        }

        // 3. 获取最新的收藏数
        Article updatedArticle = articleMapper.findById(articleId);
        Integer collectCount = updatedArticle.getCollectCount() != null ? updatedArticle.getCollectCount() : 0;

        // 4. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("collected", collected);
        result.put("collectCount", collectCount);

        return result;
    }
}