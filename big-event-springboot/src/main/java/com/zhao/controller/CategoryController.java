package com.zhao.controller;

import com.zhao.pojo.Category;
import com.zhao.pojo.CategoryVO;
import com.zhao.pojo.Result;
import com.zhao.service.CategoryService;
import com.zhao.utils.JwtUtil;
import com.zhao.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {


    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 添加文章分类
     * @param category 分类信息
     * @return 操作结果
     */
    @PostMapping
    public Result add(@RequestBody @Validated(Category.Add.class) Category category) {
        categoryService.add(category);
        return Result.success();
    }

    /**
     * 获取当前用户的分类列表
     * @return 分类列表
     */
    @GetMapping
    public Result<List<Category>> list() {
        List<Category> cs = categoryService.list();
        return Result.success(cs);
    }

    /**
     * 获取分类详情
     * @param id 分类ID
     * @return 分类详情信息
     */
    @GetMapping("/detail")
    public Result<Category> detail(Integer id) {
       Category c =  categoryService.findById(id);
       return Result.success(c);
    }
    /**
     * 更新分类
     * @param category 分类信息
     * @return 操作结果
     */
    @PutMapping
    public Result update(@RequestBody @Validated(Category.Update.class) Category category) {
        categoryService.update(category);
        return Result.success();
    }
    /**
     * 删除分类
     * @param id 分类ID
     * @return 操作结果
     */
    @DeleteMapping
    public Result delete(Integer id) {
        categoryService.delete(id);
        return Result.success();
    }

    /**
     * 获取分类列表（支持分类筛选，无需登录也可访问）
     * @return 分类列表
     */
    @GetMapping("/list")
    public Result<List<CategoryVO>> getCategoryList() {
        try {
            // 获取用户ID - 首先尝试从ThreadLocal获取，如果没有则尝试从请求头解析token
            Integer userId = null;
            try {
                // 优先从ThreadLocal获取（如果拦截器已处理）
                Map<String, Object> userMap = ThreadLocalUtil.get();
                if (userMap != null) {
                    userId = (Integer) userMap.get("id");
                }
                
                // 如果ThreadLocal中没有用户信息，尝试从当前请求头中获取token并解析
                if (userId == null) {
                    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
                    String token = request.getHeader("Authorization");
                    if (token != null && !token.trim().isEmpty()) {
                        // 处理Bearer前缀
                        if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
                            token = token.substring(7).trim();
                        }
                        
                        // 尝试解析token
                        if (!token.isEmpty()) {
                            // 验证token是否在redis中存在且未过期
                            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
                            String redisToken = operations.get(token);
                            if (redisToken != null) {
                                Map<String, Object> claims = JwtUtil.parseToken(token);
                                if (claims != null && claims.containsKey("id")) {
                                    userId = (Integer) claims.get("id");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 用户未登录或token无效，userId保持为null
                log.debug("用户未登录或token无效访问分类列表: {}", e.getMessage());
            }

            List<CategoryVO> categoryList = categoryService.getCategoryList(userId);
            return Result.success(categoryList);
        } catch (Exception e) {
            log.error("获取分类列表失败: ", e);
            return Result.error("获取分类列表失败");
        }
    }

}
