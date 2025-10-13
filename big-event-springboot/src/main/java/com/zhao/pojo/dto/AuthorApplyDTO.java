package com.zhao.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者申请列表数据传输对象
 */
@Data
public class AuthorApplyDTO {
    private Integer id;
    private UserInfo userInfo;
    private String realName;
    private String idCard;
    private String applyDesc;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 用户信息内部类
     */
    @Data
    public static class UserInfo {
        private Integer id;
        private String username;
        private String nickname;
        private String email;
        private String userPic;
    }
}