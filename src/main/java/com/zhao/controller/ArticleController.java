package com.zhao.controller;

import com.zhao.pojo.Article;
import com.zhao.pojo.PageBean;
import com.zhao.pojo.Result;
import com.zhao.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/article")
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
        articleService.findById(id);
        return Result.success();
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
}
