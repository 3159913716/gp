package com.zhao.controller;

import com.zhao.pojo.Article;
import com.zhao.pojo.ArticleHomeVO;
import com.zhao.pojo.PageBean;
import com.zhao.pojo.Result;
import com.zhao.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/article")
@Slf4j
public class ArticleController {

//    @GetMapping("/list")
//    public Result<String> list(/*@RequestHeader(name = "Authorization") String token, HttpServletResponse response*/) {
//        //验证token
    /// /        try {
    /// /            Map<String, Object> claims = JwtUtil.parseToken(token);
    /// /            return Result.success("所有的文章数据...  ");
    /// /        } catch (Exception e) {
    /// /            //http响应状态码为401
    /// /            response.setStatus(401);
    /// /            return Result.error("未登录");
    /// /        }
//        return Result.success("所有的文章数据...  ");
//    }
    @Autowired
    private ArticleService articleService;

    @PostMapping
    public Result add(@RequestBody @Validated(Article.Add.class) Article article) {
        articleService.add(article);
        return Result.success();
    }

    /**
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @param categoryId 不是必须的参数使用@RequestParam(required = false)
     * @param state 不是必须的参数使用@RequestParam(required = false)
     * */
    @GetMapping
    public Result<PageBean<Article>> list(
            Integer pageNum ,
            Integer pageSize,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String state
    ) {
        PageBean<Article> pb = articleService.list(pageNum,pageSize,categoryId,state);
        return Result.success(pb);
    }

    @GetMapping("/detail")
    public Result<Article> detail(Integer id) {
        Article article = articleService.findById(id);
        return Result.success(article);
    }
    @PutMapping
    public Result update(@RequestBody @Validated(Article.Update.class) Article article) {
        articleService.update(article);
        return Result.success();
    }
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
}
