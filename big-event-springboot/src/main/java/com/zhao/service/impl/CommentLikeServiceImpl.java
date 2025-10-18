package com.zhao.service.impl;

import com.zhao.mapper.ArticleCommentMapper;
import com.zhao.mapper.CommentLikeMapper;
import com.zhao.pojo.ArticleComment;
import com.zhao.pojo.CommentLike;
import com.zhao.service.CommentLikeService;
import com.zhao.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class CommentLikeServiceImpl implements CommentLikeService {
    
    @Autowired
    private CommentLikeMapper commentLikeMapper;
    
    @Autowired
    private ArticleCommentMapper articleCommentMapper;
    
    @Override
    @Transactional
    public Map<String, Object> likeComment(Integer commentId) {
        // 1. 获取当前登录用户ID
        Map<String, Object> userMap = ThreadLocalUtil.get();
        Integer userId = (Integer) userMap.get("id");
        
        // 2. 验证评论是否存在且未被删除
        ArticleComment comment = articleCommentMapper.selectCommentById(commentId);
        if (comment == null || comment.getIsDeleted() == 1) {
            throw new RuntimeException("评论不存在或已被删除");
        }
        
        // 3. 查询是否已点赞
        CommentLike existingLike = commentLikeMapper.findByCommentIdAndUserId(commentId, userId);
        boolean liked;
        
        if (existingLike == null) {
            // 4. 未点赞过，创建新点赞记录
            CommentLike newLike = new CommentLike();
            newLike.setCommentId(commentId);
            newLike.setUserId(userId);
            newLike.setCreateTime(LocalDateTime.now());
            newLike.setIsDeleted(0);
            commentLikeMapper.insert(newLike);
            
            // 5. 增加评论点赞数
            commentLikeMapper.updateCommentLikeCount(commentId, 1);
            liked = true;
        } else if (existingLike.getIsDeleted() == 1) {
            // 6. 点赞记录已被逻辑删除，恢复点赞
            commentLikeMapper.restoreByCommentIdAndUserId(commentId, userId);
            
            // 7. 增加评论点赞数
            commentLikeMapper.updateCommentLikeCount(commentId, 1);
            liked = true;
        } else {
            // 8. 已点赞，取消点赞（逻辑删除）
            commentLikeMapper.deleteByCommentIdAndUserId(commentId, userId);
            
            // 9. 减少评论点赞数
            commentLikeMapper.updateCommentLikeCount(commentId, -1);
            liked = false;
        }
        
        // 10. 获取更新后的点赞数
        Integer likeCount = commentLikeMapper.getCommentLikeCount(commentId);
        
        // 11. 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", likeCount);
        
        return result;
    }
}