package com.zhao.controller;

import com.zhao.pojo.Result;
import com.zhao.service.CommentLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/comment")
public class CommentLikeController {
    
    @Autowired
    private CommentLikeService commentLikeService;
    
    /**
     * 点赞/取消点赞评论
     * @param commentId 评论ID
     * @return 响应结果
     */
    @PostMapping("/{id}/like")
    public Result likeComment(@PathVariable("id") Integer commentId) {
        Map<String, Object> result = commentLikeService.likeComment(commentId);
        return Result.success(result);
    }
}