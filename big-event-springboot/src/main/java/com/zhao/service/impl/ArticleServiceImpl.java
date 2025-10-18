package com.zhao.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhao.mapper.ArticleMapper;
import com.zhao.pojo.Article;
import com.zhao.pojo.ArticleHomeVO;
import com.zhao.pojo.PageBean;
import com.zhao.service.ArticleService;
import com.zhao.utils.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    private static final String PUBLISHED_STATE = "已发布";

    @Override
    public void add(Article article) {
        //补充属性值
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        article.setCreateUser(userId);
        articleMapper.add(article);
    }

    @Override
    public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
        //1.创建PageBean对象
        PageBean<Article> pb = new PageBean<>();
        //2.开启分页查询 PageHelper插件
        PageHelper.startPage(pageNum, pageSize);
        //调用mapper完成查询
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        List<Article> as = articleMapper.list(userId, categoryId, state);
        //Page提供了方法，可以获取PageHelper分页查询后得到的总记录条数和当前页数据
        Page<Article> p = (Page<Article>) as;
        //把数据填充到PageBean对象中
        pb.setTotal(p.getTotal());
        pb.setItem(p.getResult());
        return pb;
    }

    @Override
    public Article findById(Integer id) {
         Article a = articleMapper.findById(id);
        return a;
    }

    @Override
    public void update(Article article) {
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.update(article);
    }

    @Override
    public void delete(Integer id) {
        articleMapper.delete(id);
    }

    /**
     * 获取首页文章列表业务实现
     * 包含完整的参数校验、数据查询、异常处理逻辑
     *
     * @param page 当前页码
     * @param pageSize 每页大小
     * @param sort 排序方式
     * @return 分页结果对象
     */
    @Override
    public PageBean<ArticleHomeVO> getHomeArticles(Integer page, Integer pageSize, String sort) {
        // 实现逻辑


        try {

            // ========== 参数校验和规范化 ==========
            // 如果每页大小为空或者小于1，就设置为默认值10
            // 同时限制最大不能超过50条，防止一次查询太多数据拖慢系统
            page = (page == null || page < 1) ? 1 : page;
            // 页大小校验：确保页大小在合理范围内，为空时默认为10
            pageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;
            // 限制最大页大小，防止恶意查询导致性能问题
            pageSize = Math.min(pageSize, 50);


            // 排序方式只允许两种：new(按时间最新) 或 hot(按热度)
            // 如果用户传了其他值，或者没传值，就默认用new
            if (!StringUtils.hasText(sort) ||
                    (!"new".equalsIgnoreCase(sort) && !"hot".equalsIgnoreCase(sort))) {
                sort = "new";
            } else {
                // 统一转为小写，避免大小写问题
                sort = sort.toLowerCase();
            }


            // ========== 数据查询处理 ==========
            // 比如第1页，每页10条，就从第0条开始查
            // 比如第2页，每页10条，就从第10条开始查
            int offset = (page - 1) * pageSize;
            // 查询数据
            // 调用Mapper方法，传入排序方式、文章状态(只查已发布的)、起始位置、每页条数
            List<ArticleHomeVO> articleList = articleMapper.selectHomeArticles(
                    sort, "已发布", offset, pageSize);
            // 这个数字用于前端显示总页数
            Long total = articleMapper.countHomeArticles("已发布");
            // 把查询到的文章列表、总条数、当前页码、每页大小打包返回
            return new PageBean<>(articleList, total, page, pageSize);

        } catch (Exception e) {
            // ========== 异常处理：如果上面任何一步出错了 ==========
            // 在日志里记录详细的错误信息，方便排查问题
            log.error("获取首页文章列表失败: ", e);
            // 给用户返回一个友好的错误提示，而不是一堆看不懂的技术细节
            throw new RuntimeException("获取文章列表失败");
        }

    }


    
    /**
     * 搜索文章业务实现
     * @param keyword 搜索关键词
     * @param page 当前页码
     * @param pageSize 每页大小
     * @return 搜索结果
     */
    @Override
    public PageBean<ArticleHomeVO> searchArticles(String keyword, Integer page, Integer pageSize) {
        try {
            // 参数校验和规范化
            page = (page == null || page < 1) ? 1 : page;
            pageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;
            pageSize = Math.min(pageSize, 50); // 限制最大页大小
            
            // 计算分页偏移量
            int offset = (page - 1) * pageSize;
            
            // 调用Mapper查询搜索结果
            List<ArticleHomeVO> articleList = articleMapper.searchArticles(keyword, "已发布", offset, pageSize);
            
            // 查询匹配的总条数
            Long total = articleMapper.countSearchArticles(keyword, "已发布");
            
            // 返回分页结果
            return new PageBean<>(articleList, total, page, pageSize);
        } catch (Exception e) {
            log.error("搜索文章失败: ", e);
            throw new RuntimeException("搜索失败，请稍后重试");
        }
    }
    
    @Override
    public PageBean<Article> getUserArticles(Integer page, Integer pageSize, String state) {
        try {
            // 参数校验和规范化
            page = (page == null || page < 1) ? 1 : page;
            pageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;
            pageSize = Math.min(pageSize, 50); // 限制最大页大小
            
            // 获取当前用户ID
            Map<String, Object> map = ThreadLocalUtil.get();
            Integer userId = (Integer) map.get("id");
            
            // 计算分页偏移量
            int offset = (page - 1) * pageSize;
            
            // 查询用户文章列表
            List<Article> articleList = articleMapper.getUserArticles(userId, state, offset, pageSize);
            
            // 查询用户文章总数
            Long total = articleMapper.countUserArticles(userId, state);
            
            // 返回分页结果
            return new PageBean<>(articleList, total, page, pageSize);
        } catch (Exception e) {
            log.error("获取用户文章列表失败: ", e);
            throw new RuntimeException("获取文章列表失败，请稍后重试");
        }
    }
}

