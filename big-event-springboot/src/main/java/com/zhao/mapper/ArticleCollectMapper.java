package com.zhao.mapper;

import com.zhao.pojo.ArticleCollect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleCollectMapper {

    /**
     * 根据文章ID和用户ID查询收藏记录
     * @param articleId 文章ID
     * @param userId 用户ID
     * @return 收藏记录
     */
    ArticleCollect findByArticleIdAndUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    /**
     * 插入收藏记录
     * @param articleCollect 收藏记录
     */
    void insert(ArticleCollect articleCollect);

    /**
     * 逻辑删除收藏记录
     * @param articleId 文章ID
     * @param userId 用户ID
     */
    void deleteByArticleIdAndUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);

    /**
     * 恢复收藏记录（取消删除）
     * @param articleId 文章ID
     * @param userId 用户ID
     */
    void restoreByArticleIdAndUserId(@Param("articleId") Integer articleId, @Param("userId") Integer userId);
}