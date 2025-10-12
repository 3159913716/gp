package com.zhao.controller;

import com.zhao.pojo.ArticleHomeVO;
import com.zhao.pojo.PageBean;
import com.zhao.pojo.Result;
import com.zhao.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 搜索相关控制器
 */
@RestController
@RequestMapping("/search")
@Slf4j
public class SearchController {

    @Autowired
    private ArticleService articleService;

    /**
     * 搜索文章接口
     * @param keyword 搜索关键词
     * @param page 当前页码
     * @param pageSize 每页条数
     * @return 搜索结果
     */
    @GetMapping
    public Result<Map<String, Object>> searchArticles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        try {
            // 记录搜索请求日志
            log.info("搜索文章 - keyword: {}, page: {}, pageSize: {}", keyword, page, pageSize);
            
            // 对关键词进行trim和安全性检查
            if (!StringUtils.hasText(keyword)) {
                return Result.error("搜索关键词不能为空");
            }
            
            keyword = keyword.trim();
            
            // 调用业务层进行搜索
            PageBean<ArticleHomeVO> searchResult = articleService.searchArticles(keyword, page, pageSize);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("list", searchResult.getItem());
            result.put("total", searchResult.getTotal());
            result.put("keyword", keyword);
            
            return Result.success(result);
        } catch (Exception e) {
            // 异常处理
            log.error("搜索文章失败: ", e);
            return Result.error("搜索失败，请稍后重试");
        }
    }
}