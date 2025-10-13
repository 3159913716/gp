package com.zhao.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员操作日志实体类
 */
@Data
public class AdminOperationLog {
    private Integer id;
    private Integer adminId;
    private String operationContent;
    private LocalDateTime operationTime;
    private String ipAddress;
}