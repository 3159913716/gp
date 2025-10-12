package com.zhao.service;

import com.zhao.pojo.Article;
import com.zhao.pojo.ArticleHomeVO;
import com.zhao.pojo.PageBean;

public interface ArticleService {

    //新增文章
    void add(Article article);
    //条件分页列表查询
    PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state);

    //根据文章id获取文章详情
    Article findById(Integer id);

    //更新文章
    void update(Article article);
    //删除文章
    void delete(Integer id);


    /**
     * 获取首页文章列表
     * 这个方法用于从数据库查询文章信息，支持分页和不同排序方式
     * 主要用在网站首页、文章列表页等需要展示多篇文章的场景
     * @param page 当前要看第几页，从1开始计数。比如传1就是看第一页
     * @param pageSize 每页显示多少篇文章。比如传10就是每页10篇文章
     * @param sort 排序方式："new"按发布时间最新，"hot"按文章热度(点赞+收藏)
     * @return 返回一个分页对象，里面包含：文章列表 + 总文章数 + 当前页码 + 每页大小
     * 使用示例：
     * getHomeArticles(1, 10, "new")    // 查看第1页，每页10条，按最新排序
     * getHomeArticles(2, 5, "hot")     // 查看第2页，每页5条，按热度排序
     */
    PageBean<ArticleHomeVO> getHomeArticles(Integer page, Integer pageSize, String sort);
    
    
    /**
     * 搜索文章
     * 根据关键词在文章标题和内容中进行搜索
     * @param keyword 搜索关键词
     * @param page 当前页码
     * @param pageSize 每页条数
     * @return 搜索结果，包含文章列表、总数等信息
     */
    PageBean<ArticleHomeVO> searchArticles(String keyword, Integer page, Integer pageSize);
}
