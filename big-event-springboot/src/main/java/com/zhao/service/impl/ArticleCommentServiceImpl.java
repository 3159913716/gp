package com.zhao.service.impl;

import com.zhao.mapper.ArticleCommentMapper;
import com.zhao.mapper.ArticleMapper;
import com.zhao.mapper.UserMapper;
import com.zhao.pojo.Article;
import com.zhao.pojo.ArticleComment;
import com.zhao.pojo.ArticleCommentVO;
import com.zhao.pojo.User;
import com.zhao.service.ArticleCommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class ArticleCommentServiceImpl implements ArticleCommentService {

    @Autowired
    private ArticleCommentMapper articleCommentMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    private static final String PUBLISHED_STATE = "已发布";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleCommentVO addComment(ArticleComment articleComment, Integer userId) {
        // 1. 验证文章是否存在且状态为"已发布"
        Article article = articleMapper.findById(articleComment.getArticleId());
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!PUBLISHED_STATE.equals(article.getState())) {
            throw new RuntimeException("只能评论已发布的文章");
        }

        // 2. 验证父评论（如果存在）
        Integer parentId = articleComment.getParentId();
        if (parentId != null && parentId > 0) {
            // 获取父评论所属的文章ID
            Integer parentCommentArticleId = articleCommentMapper.getArticleIdByCommentId(parentId);
            
            // 验证父评论是否存在且未被删除
            if (parentCommentArticleId == null) {
                throw new RuntimeException("父评论不存在或已被删除");
            }
            
            // 验证父评论是否属于当前文章（防止跨文章回复）
            if (!articleComment.getArticleId().equals(parentCommentArticleId)) {
                throw new RuntimeException("只能在原评论所属文章下回复评论");
            }
        } else {
            // 设置默认值为0（一级评论）
            articleComment.setParentId(0);
        }

        // 3. 设置评论信息
        articleComment.setUserId(userId);
        articleComment.setCreateTime(LocalDateTime.now());
        articleComment.setIsDeleted(0);
        articleComment.setCommentLikeCount(0); // 初始点赞数为0

        // 4. 保存评论
        articleCommentMapper.insert(articleComment);
        
        // 5. 获取用户信息
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户信息不存在");
        }
        
        // 6. 构建返回的VO对象
        ArticleCommentVO commentVO = new ArticleCommentVO();
        commentVO.setId(articleComment.getId());
        commentVO.setContent(articleComment.getContent());
        commentVO.setParentId(articleComment.getParentId());
        commentVO.setCreateTime(articleComment.getCreateTime());
        
        // 设置用户信息
        commentVO.setNickname(user.getNickname());
        commentVO.setUserPic(user.getUserPic());
        
        log.info("用户{}发布评论成功，评论ID：{}", userId, articleComment.getId());
        
        return commentVO;
    }



    
    @Override
    public HashMap<String, Object> getArticleComments(Integer articleId, Integer page, Integer pageSize, Integer userId) {
        // 参数验证与纠错
        if (articleId == null || articleId <= 0) {
            throw new RuntimeException("无效的文章ID");
        }
        
        // 页码纠错：如果页码小于1，则自动设置为1
        if (page == null || page < 1) {
            page = 1;
        }
        
        // 每页条数纠错：如果每页条数小于1，则设置为默认值10；如果大于100，则设置为100
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        } else if (pageSize > 100) {
            pageSize = 100;
        }
        
        // 验证文章是否存在
        Article article = articleMapper.findById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        // 计算分页参数
        int start = (page - 1) * pageSize;
        
        // 获取一级评论列表
        List<Map<String, Object>> mainComments = articleCommentMapper.getArticleMainComments(articleId, start, pageSize);
        
        // 获取评论总数
        Integer total = articleCommentMapper.getArticleMainCommentsCount(articleId);
        
        // 构建评论ID列表，用于批量查询点赞状态
        List<Integer> commentIds = new ArrayList<>();
        for (Map<String, Object> comment : mainComments) {
            // 处理Long到Integer的类型转换
            Object idObj = comment.get("id");
            Integer id = null;
            if (idObj instanceof Long) {
                id = ((Long) idObj).intValue();
            } else if (idObj instanceof Integer) {
                id = (Integer) idObj;
            }
            if (id != null) {
                commentIds.add(id);
            }
        }
        
        // 获取用户点赞的评论ID列表（如果用户已登录）
        Set<Integer> likedCommentIds = new HashSet<>();
        if (userId != null && !commentIds.isEmpty()) {
            List<Integer> likedIds = articleCommentMapper.getLikedCommentIds(userId, commentIds);
            if (likedIds != null) {
                likedCommentIds.addAll(likedIds);
            }
        }
        
        // 构建评论树形结构，包含回复
        List<Map<String, Object>> commentList = new ArrayList<>();
        for (Map<String, Object> mainComment : mainComments) {
            // 构建评论对象
            Map<String, Object> comment = new HashMap<>(mainComment);
            
            // 处理ID类型转换
            Object idObj = comment.get("id");
            Integer id = null;
            if (idObj instanceof Long) {
                id = ((Long) idObj).intValue();
            } else if (idObj instanceof Integer) {
                id = (Integer) idObj;
            }
            if (id != null) {
                comment.put("id", id);
            }
            
            // 设置点赞状态
            comment.put("liked", likedCommentIds.contains(id));
            
            // 构建用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("nickname", comment.remove("nickname"));
            userInfo.put("userPic", comment.remove("userPic"));
            comment.put("userInfo", userInfo);
            
            // 获取回复列表
            if (id != null) {
                List<Map<String, Object>> replies = articleCommentMapper.getCommentReplies(id);
                
                // 处理回复的用户信息和点赞状态
                List<Map<String, Object>> processedReplies = new ArrayList<>();
                for (Map<String, Object> reply : replies) {
                    Map<String, Object> processedReply = new HashMap<>(reply);
                    
                    // 处理回复ID类型转换
                    Object replyIdObj = reply.get("id");
                    Integer replyId = null;
                    if (replyIdObj instanceof Long) {
                        replyId = ((Long) replyIdObj).intValue();
                    } else if (replyIdObj instanceof Integer) {
                        replyId = (Integer) replyIdObj;
                    }
                    if (replyId != null) {
                        processedReply.put("id", replyId);
                    }
                    
                    // 设置点赞状态
                    processedReply.put("liked", likedCommentIds.contains(replyId));
                    
                    // 构建回复的用户信息
                    Map<String, Object> replyUserInfo = new HashMap<>();
                    replyUserInfo.put("nickname", processedReply.remove("nickname"));
                    replyUserInfo.put("userPic", processedReply.remove("userPic"));
                    processedReply.put("userInfo", replyUserInfo);
                    
                    processedReplies.add(processedReply);
                }
                
                comment.put("replies", processedReplies);
            }
            commentList.add(comment);
        }
        
        // 构建返回结果
        HashMap<String, Object> result = new HashMap<>();
        result.put("list", commentList);
        result.put("total", total);
        
        return result;
    }
}