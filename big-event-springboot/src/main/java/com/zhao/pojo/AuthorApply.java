package com.zhao.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者申请表实体类
 */
@Data
public class AuthorApply {
    private Integer id; // 主键，自增
    private Integer userId; // 外键，关联USER表id
    
    @NotBlank(message = "真实姓名不能为空")
    private String realName; // 申请人真实姓名
    
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$", 
             message = "身份证号格式不正确")
    private String idCard; // 身份证号，加密存储
    
    private String applyDesc; // 申请描述
    
    private Integer status; // 申请状态，0=待审核/1=通过/2=拒绝
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 申请时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime; // 审核时间
    
    private Integer auditUserId; // 外键，关联USER表id，对应审核管理员
    
    private String rejectReason; // 拒绝原因
}