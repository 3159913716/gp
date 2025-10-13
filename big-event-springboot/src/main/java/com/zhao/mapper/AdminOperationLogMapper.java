package com.zhao.mapper;

import com.zhao.pojo.AdminOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员操作日志Mapper接口
 */
@Mapper
public interface AdminOperationLogMapper {

    /**
     * 插入管理员操作日志
     * @param log 日志对象
     */
    void insert(AdminOperationLog log);
}