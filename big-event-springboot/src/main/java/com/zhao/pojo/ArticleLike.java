package com.zhao.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ArticleLike {
    private Integer id;
    private Integer articleId;
    private Integer userId;
    
    /**
     * 点赞时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    /**
     * 是否删除（逻辑删除）
     * 0: 未删除
     * 1: 已删除
     */
    private Integer isDeleted = 0;
}