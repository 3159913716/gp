package com.zhao.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ArticleCommentVO {
    private Integer id;
    private String content;
    private Integer parentId = 0;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像
     */
    private String userPic;
    
    /**
     * 评论点赞数
     */
    private Integer commentLikeCount;
}