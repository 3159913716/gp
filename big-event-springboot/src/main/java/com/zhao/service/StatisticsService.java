package com.zhao.service;

import java.util.Map;

/**
 * 数据统计服务接口
 */
public interface StatisticsService {

    /**
     * 获取数据统计信息
     * @return 包含用户数、文章数、活跃用户等统计数据
     */
    Map<String, Object> getStatistics();
}