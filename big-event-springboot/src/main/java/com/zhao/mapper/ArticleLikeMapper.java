package com.zhao.mapper;

import com.zhao.pojo.ArticleLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleLikeMapper {
    
    /**
     * 根据文章ID和用户ID查询点赞记录
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 点赞记录
     */
    ArticleLike findByArticleIdAndUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);
    
    /**
     * 插入点赞记录
     * @param articleLike 点赞记录
     */
    void insert(ArticleLike articleLike);
    
    /**
     * 逻辑删除点赞记录
     * @param articleId 文章ID
     * @param userId 用户ID
     */
    void deleteByArticleIdAndUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);
    
    /**
     * 恢复点赞记录（取消删除）
     * @param articleId 文章ID
     * @param userId 用户ID
     */
    void restoreByArticleIdAndUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);
}