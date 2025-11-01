package com.zhao.controller;

import com.zhao.pojo.Category;
import com.zhao.pojo.CategoryVO;
import com.zhao.pojo.Result;
import com.zhao.service.CategoryService;
import com.zhao.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {


    @Autowired
    private CategoryService categoryService;


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
            // 使用UserContextUtil获取用户ID，自动处理登录状态检测
            Integer userId = UserContextUtil.getCurrentUserId();
            log.debug("获取分类列表，用户ID: {}", userId);
            List<CategoryVO> categoryList = categoryService.getCategoryList(userId);
            return Result.success(categoryList);
        } catch (Exception e) {
            log.error("获取分类列表失败: ", e);
            return Result.error("获取分类列表失败");
        }
    }

}
