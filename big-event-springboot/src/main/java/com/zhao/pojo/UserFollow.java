package com.zhao.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserFollow {
    private Integer id;
    
    /**
     * 关注者ID (follower_id)
     */
    private Integer followerId;
    
    /**
     * 被关注者ID (followed_id)
     */
    private Integer followedId;
    
    /**
     * 关注时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 是否删除（逻辑删除）
     * 0: 未删除
     * 1: 已删除
     */
    private Integer isDeleted = 0;
}