package com.zhao.mapper;

import com.zhao.pojo.Article;
import com.zhao.pojo.ArticleHomeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ArticleMapper {
    //新增文章
    void add(Article article);

    List<Article> list(Integer userId, Integer categoryId, String state);

    //根据id查找文章
    Article findById(Integer id);

    //修改文章
    void update(Article article);

    //删除文章
    void delete(Integer id);

    List<ArticleHomeVO> selectHomeArticles(
            @Param("sort") String sort,
            @Param("state") String state,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    Long countHomeArticles(@Param("state") String state);
    
    // 搜索文章
    List<ArticleHomeVO> searchArticles(
            @Param("keyword") String keyword,
            @Param("state") String state,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );
    
    // 统计搜索结果总数
    Long countSearchArticles(
            @Param("keyword") String keyword,
            @Param("state") String state
    );
    
    /**
     * 增加文章点赞数
     * @param articleId 文章ID
     */
    void incrementLikeCount(@Param("articleId") Integer articleId);
    
    /**
     * 减少文章点赞数
     * @param articleId 文章ID
     */
    void decrementLikeCount(@Param("articleId") Integer articleId);
    
    /**
     * 增加文章收藏数
     * @param articleId 文章ID
     */
    void incrementCollectCount(@Param("articleId") Integer articleId);
    
    /**
     * 减少文章收藏数
     * @param articleId 文章ID
     */
    void decrementCollectCount(@Param("articleId") Integer articleId);
    
    // 获取文章总数
    Integer getTotalArticles();
    
    // 获取今日新增文章数
    Integer getTodayNewArticles(@Param("todayStart") LocalDateTime todayStart);
    
    // 根据日期范围获取新增文章数
    Integer getNewArticlesByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    /**
     * 获取用户文章列表（带分页和状态筛选）
     * @param userId 用户ID
     * @param state 文章状态
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 文章列表
     */
    List<Article> getUserArticles(@Param("userId") Integer userId, @Param("state") String state, 
                                 @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);
    
    /**
     * 统计用户文章总数（带状态筛选）
     * @param userId 用户ID
     * @param state 文章状态
     * @return 文章总数
     */
    Long countUserArticles(@Param("userId") Integer userId, @Param("state") String state);

}
