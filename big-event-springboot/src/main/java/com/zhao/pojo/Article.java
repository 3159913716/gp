package com.zhao.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhao.anno.State;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.groups.Default;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import java.time.LocalDateTime;

@Data
public class Article {
    @NotNull(groups = {Update.class})
    private Integer id;
    @NotEmpty//非空
    @Pattern(regexp = "^\\S{1,10}$")//1~10个字符
    private String title;
    @NotEmpty
    private String content;
    @NotEmpty
    @URL//需要是url地址
    private String coverImg;
    @State
    private String state;
    @NotNull
    private Integer categoryId;
    private Integer createUser;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 文章点赞数 - 用于避免频繁联表查询 article_like 表
     * 默认值: 0
     */
    private Integer likeCount = 0;

    /**
     * 文章收藏数 - 用于避免频繁联表查询 article_collect 表
     * 默认值: 0
     */
    private Integer collectCount = 0;

    // === 验证分组接口 ===

    public interface Add extends Default {

    }
    public interface Update extends Default {}
}