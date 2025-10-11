package com.zhao.pojo;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    /**
     * NotNull 值不能为null
     * */
    @NotNull
    private Integer id;
    private String username;

    /**让springmvc把当前对象转换成json字符串的时候,忽略password,最终的json字符串中就没有password这个属性了
     * 注意导包:import com.fasterxml.jackson.annotation.JsonIgnore;
     * */
    @JsonIgnore
    private String password;

    /**
     * NotEmpty 值不能为null,且内容不能为空
     * */
    @NotEmpty
    @Pattern(regexp = "^\\S{1,10}$")
    private String nickname;


    /**
     * Email 满足邮箱格式
     * */
    @NotEmpty
    @Email
    private String email;
    private String userPic;//用户头像地址
    private LocalDateTime createTime;//创建时间
    private LocalDateTime updateTime;//更新时间
    
    // 用户角色：0-管理员，1-作者，2-普通用户
    private Integer role = 2;
    
    // 账号状态：0-正常，1-禁用
    private Integer status = 0;

}