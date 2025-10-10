package com.zhao.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleHomeVO {
    private Integer id;
    private String title;
    private String coverImg;
    private String summary;
    private String author;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private Integer likeCount;
    private Integer collectCount;
    private Integer commentCount;
    private String state;
    private Integer categoryId;
}
