package com.zhao.service;

import com.zhao.pojo.AdminOperationLog;

/**
 * 管理员操作日志服务接口
 */
public interface AdminOperationLogService {
    
    /**
     * 记录管理员操作日志
     * @param adminId 管理员ID
     * @param operationContent 操作内容
     * @param ipAddress 操作IP地址
     */
    void recordLog(Integer adminId, String operationContent, String ipAddress);
}