package com.zhao.service.impl;

import com.zhao.mapper.AdminOperationLogMapper;
import com.zhao.pojo.AdminOperationLog;
import com.zhao.service.AdminOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理员操作日志服务实现类
 */
@Service
public class AdminOperationLogServiceImpl implements AdminOperationLogService {
    
    @Autowired
    private AdminOperationLogMapper adminOperationLogMapper;
    
    @Override
    public void recordLog(Integer adminId, String operationContent, String ipAddress) {
        // 创建日志对象
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(adminId);
        log.setOperationContent(operationContent);
        log.setOperationTime(LocalDateTime.now());
        log.setIpAddress(ipAddress);
        
        // 插入日志记录
        adminOperationLogMapper.insert(log);
    }
}