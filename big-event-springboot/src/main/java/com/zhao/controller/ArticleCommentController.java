package com.zhao.controller;

import com.zhao.pojo.ArticleComment;
import com.zhao.pojo.ArticleCommentVO;
import com.zhao.pojo.Result;
import com.zhao.service.ArticleCommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static com.zhao.utils.ThreadLocalUtil.get;

@RestController
@RequestMapping("/article")
@Slf4j
public class ArticleCommentController {

    @Autowired
    private ArticleCommentService articleCommentService;

    /**
     * 发布评论接口
     * @param id 文章ID
     * @param articleComment 评论内容
     * @return 评论结果
     */
    @PostMapping("/{id}/comment")
    public Result<ArticleCommentVO> publishComment(@PathVariable Integer id, @RequestBody ArticleComment articleComment) {
        try {
            // 显式检查评论内容是否为空
            if (articleComment.getContent() == null || articleComment.getContent().trim().isEmpty()) {
                return Result.error("评论内容不能为空，请输入您的评论");
            }
            
            // 1. 从ThreadLocal中获取当前登录用户ID
            Map<String, Object> userMap = get();
            Integer userId = (Integer) userMap.get("id");
            
            // 2. 设置文章ID
            articleComment.setArticleId(id);
            
            // 3. 调用服务层发布评论
            ArticleCommentVO commentVO = articleCommentService.addComment(articleComment, userId);
            
            return Result.success(commentVO);
        } catch (Exception e) {
            log.error("用户ID: {}, 文章ID: {}, 发布评论失败: {}, 错误详情: {}", 
                    articleComment != null && get() != null ? (Integer)((Map<String, Object>)get()).get("id") : null, 
                    id, e.getMessage(), e);
            // 对常见错误类型返回更友好的信息
            String message = e.getMessage();
            if (message != null && message.contains("评论内容不能为空")) {
                return Result.error("评论内容不能为空，请输入您的评论");
            }
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取文章评论列表
     */
    @GetMapping("/{id}/comments")
    public Result<HashMap<String, Object>> getArticleComments(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 获取当前登录用户ID（如果未登录则为null）
        Integer userId = null;
        try {
            Map<String, Object> userMap = get();
            userId = (Integer) userMap.get("id");
        } catch (Exception e) {
            // 用户未登录，不做处理
        }
        
        // 调用服务层获取评论列表
        HashMap<String, Object> comments = articleCommentService.getArticleComments(id, page, pageSize, userId);
        
        return Result.success(comments);
    }
}