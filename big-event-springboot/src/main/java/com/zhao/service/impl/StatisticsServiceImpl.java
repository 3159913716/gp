package com.zhao.service.impl;

import com.zhao.mapper.ArticleMapper;
import com.zhao.mapper.UserMapper;
import com.zhao.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计服务实现类
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取当前日期的开始时间
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        
        // 1. 获取总用户数
        Integer totalUsers = userMapper.getTotalUsers();
        
        // 2. 获取总文章数
        Integer totalArticles = articleMapper.getTotalArticles();
        
        // 3. 获取今日新增用户数
        Integer todayNewUsers = userMapper.getTodayNewUsers(todayStart);
        
        // 4. 获取今日新增文章数
        Integer todayNewArticles = articleMapper.getTodayNewArticles(todayStart);
        
        // 5. 计算日活跃用户数（这里简化处理，实际可能需要根据登录日志或操作日志统计）
        // 暂时使用一个估算值，实际应用中需要实现具体的统计逻辑
        Integer dailyActiveUsers = userMapper.getDailyActiveUsers(todayStart);
        
        // 6. 获取近7天数据趋势
        Map<String, Object> trendData = getTrendData();
        
        // 组装返回结果
        result.put("totalUsers", totalUsers);
        result.put("totalArticles", totalArticles);
        result.put("todayNewUsers", todayNewUsers);
        result.put("todayNewArticles", todayNewArticles);
        result.put("dailyActiveUsers", dailyActiveUsers);
        result.put("trendData", trendData);
        
        return result;
    }
    
    /**
     * 获取近7天数据趋势
     * @return 包含日期、用户数、文章数的趋势数据
     */
    private Map<String, Object> getTrendData() {
        Map<String, Object> trendData = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<Integer> users = new ArrayList<>();
        List<Integer> articles = new ArrayList<>();
        
        // 生成近7天的日期
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate now = LocalDate.now();
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = now.minusDays(i);
            String dateStr = date.format(formatter);
            dates.add(dateStr);
            
            // 获取某天的新增用户数
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            
            Integer dailyUsers = userMapper.getNewUsersByDateRange(startOfDay, endOfDay);
            Integer dailyArticles = articleMapper.getNewArticlesByDateRange(startOfDay, endOfDay);
            
            users.add(dailyUsers);
            articles.add(dailyArticles);
        }
        
        trendData.put("dates", dates);
        trendData.put("users", users);
        trendData.put("articles", articles);
        
        return trendData;
    }
}