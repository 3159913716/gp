package com.zhao.controller;

import com.zhao.pojo.Article;
import com.zhao.pojo.ArticleDetailVO;
import com.zhao.pojo.ArticleHomeVO;
import com.zhao.pojo.PageBean;
import com.zhao.pojo.Result;
import com.zhao.service.ArticleLikeService;
import com.zhao.service.ArticleCollectService;
import com.zhao.service.ArticleService;
import com.zhao.utils.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/article")
@Slf4j
public class ArticleController {

    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private ArticleLikeService articleLikeService;
    
    @Autowired
    private ArticleCollectService articleCollectService;

    /**
     * 添加文章
     * @param article 文章信息
     * @return 操作结果
     */
    @PostMapping
    public Result add(@RequestBody @Validated(Article.Add.class) Article article) {
        articleService.add(article);
        return Result.success();
    }

    /**
     * 公开的文章详情接口（无需登录即可访问）
     * @param id 文章ID
     * @return 文章详情信息
     */
    @GetMapping("/public-detail/{id}")
    public Result<Article> publicDetail(@PathVariable Integer id) {
        Article article = articleService.findById(id);
        return Result.success(article);
    }
  
    /**
     * 获取文章列表（支持分页、分类筛选和状态筛选）
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @param categoryId 分类ID（可选）
     * @param state 文章状态（可选）
     * @return 文章列表和分页信息
     */
    @GetMapping
    public Result<PageBean<Article>> list(
            Integer pageNum,
            Integer pageSize,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String state
    ) {
        PageBean<Article> pb = articleService.list(pageNum, pageSize, categoryId, state);
        return Result.success(pb);
    }

    /**
     * 获取文章详情
     * @param id 文章ID
     * @return 文章详情信息
     */
    @GetMapping("/detail")
    public Result<Article> detail(Integer id) {
        Article article = articleService.findById(id);
        return Result.success(article);
    }
    
    /**
     * 获取文章详情页信息（支持未登录访问）
     * @param id 文章ID
     * @return 文章详情页信息，包含点赞收藏状态（自动从token获取用户信息）
     */
    @GetMapping("/detail-page")
    public Result<ArticleDetailVO> detailPage(@RequestParam Integer id) {
        try {
            // 调用service层获取文章详情，service内部会处理用户登录状态
            ArticleDetailVO articleDetailVO = articleService.getArticleDetail(id);
            return Result.success(articleDetailVO);
        } catch (Exception e) {
            log.error("获取文章详情失败: ", e);
            return Result.error("获取文章详情失败，请稍后重试");
        }
    }
    
    /**
     * 更新文章
     * @param article 文章信息
     * @return 操作结果
     */
    @PutMapping
    public Result update(@RequestBody @Validated(Article.Update.class) Article article) {
        articleService.update(article);
        return Result.success();
    }
    
    /**
     * 删除文章
     * @param id 文章ID
     * @return 操作结果
     */
    @DeleteMapping
    public Result delete(Integer id) {
        articleService.delete(id);
        return Result.success();
    }


    @GetMapping("/home")
    public Result<PageBean<ArticleHomeVO>> getHomeArticles(
            // 页码参数，如果不传默认显示第1页,以下类似
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "new") String sort) {
        // 控制器逻辑


        try {
            // 记录一下谁在什么时候调用了这个接口，方便排查问题
            log.info("获取首页文章列表 - page: {}, pageSize: {}, sort: {}", page, pageSize, sort);

            // 调用业务层方法，真正去数据库查数据
            PageBean<ArticleHomeVO> result = articleService.getHomeArticles(page, pageSize, sort);

            // 把查到的数据包装成成功格式返回给前端
            return Result.success(result);

        } catch (Exception e) {
            // 如果上面任何一步出错了，就在这里捕获
            // 在后台日志记录详细错误，方便程序员排查问题
            log.error("首页文章列表接口异常: ", e);
            return Result.error("获取文章列表失败");
        }
    }
    
    /**
     * 点赞/取消点赞文章
     * @param id 文章ID
     * @return 操作结果
     */
    @PostMapping("/like/{id}")
    public Result<Map<String, Object>> toggleLike(@PathVariable Integer id) {
        try {
            // 从ThreadLocal中获取当前用户ID
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer userId = (Integer) userMap.get("id");
            
            // 调用服务层进行点赞操作
            Map<String, Object> result = articleLikeService.toggleLike(id, userId);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("点赞操作失败: ", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 收藏/取消收藏文章
     * @param id 文章ID
     * @return 操作结果
     */
    @PostMapping("/collect/{id}")
    public Result<Map<String, Object>> toggleCollect(@PathVariable Integer id) {
        try {
            // 从ThreadLocal中获取当前用户ID
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer userId = (Integer) userMap.get("id");
            
            // 调用服务层进行收藏操作
            Map<String, Object> result = articleCollectService.toggleCollect(id, userId);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("收藏操作失败: ", e);
            return Result.error(e.getMessage());
        }
    }
}
