package com.zhao.mapper;

import com.zhao.pojo.ArticleComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleCommentMapper {
    
    /**
     * 插入评论记录
     * @param articleComment 评论信息
     */
    void insert(ArticleComment articleComment);
    
    /**
     * 验证父评论是否存在且未被删除
     * @param parentId 父评论ID
     * @return 存在返回1，不存在返回0
     */
    Integer checkParentComment(@Param("parentId") Integer parentId);
    
    /**
     * 获取父评论的文章ID
     * @param parentId 父评论ID
     * @return 文章ID，如果父评论不存在或已被删除则返回null
     */
    Integer getArticleIdByCommentId(@Param("parentId") Integer parentId);
    
    /**
     * 获取文章的一级评论列表（分页）
     * @param articleId 文章ID
     * @param start 起始索引
     * @param pageSize 每页大小
     * @return 评论列表
     */
    List<Map<String, Object>> getArticleMainComments(@Param("articleId") Integer articleId, 
                                                    @Param("start") Integer start, 
                                                    @Param("pageSize") Integer pageSize);
    
    /**
     * 获取文章的一级评论总数
     * @param articleId 文章ID
     * @return 评论总数
     */
    Integer getArticleMainCommentsCount(@Param("articleId") Integer articleId);
    
    /**
     * 获取指定评论的所有回复
     * @param parentId 父评论ID
     * @return 回复列表
     */
    List<Map<String, Object>> getCommentReplies(@Param("parentId") Integer parentId);
    
    /**
     * 查询用户点赞的评论ID列表
     * @param userId 用户ID
     * @param commentIds 评论ID列表
     * @return 点赞的评论ID列表
     */
    List<Integer> getLikedCommentIds(@Param("userId") Integer userId, @Param("commentIds") List<Integer> commentIds);
    
    /**
     * 根据评论ID查询评论信息
     * @param id 评论ID
     * @return 评论对象
     */
    ArticleComment selectCommentById(@Param("id") Integer id);
}