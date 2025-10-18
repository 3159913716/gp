package com.zhao.service;

import com.zhao.pojo.ArticleComment;
import com.zhao.pojo.ArticleCommentVO;

import java.util.HashMap;
import java.util.List;

public interface ArticleCommentService {
    
    /**
     * 发布评论
     * @param articleComment 评论信息
     * @param userId 当前用户ID
     * @return 包含评论信息的VO对象
     */
    ArticleCommentVO addComment(ArticleComment articleComment, Integer userId);
    
    /**
     * 获取文章评论列表
     * @param articleId 文章ID
     * @param page 当前页码
     * @param pageSize 每页大小
     * @param userId 当前登录用户ID，用于判断是否点赞
     * @return 评论列表及总数
     */
    HashMap<String, Object> getArticleComments(Integer articleId, Integer page, Integer pageSize, Integer userId);
}