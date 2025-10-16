package com.zhao.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ArticleComment {
    private Integer id;
    private Integer articleId;
    private Integer userId;
    @NotEmpty(message = "评论内容不能为空")
    private String content;
    private Integer parentId = 0; // 父评论ID，默认为0表示一级评论
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 是否删除（逻辑删除）
     * 0: 未删除
     * 1: 已删除
     */
    private Integer isDeleted = 0;
    
    /**
     * 评论点赞数
     */
    private Integer commentLikeCount = 0;
}