package com.zhao.controller;

import com.zhao.pojo.Category;
import com.zhao.pojo.CategoryVO;
import com.zhao.pojo.Result;
import com.zhao.service.CategoryService;
import com.zhao.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/category")
public class CategoryController {


    @Autowired
    private CategoryService categoryService;


    @PostMapping
    public Result add(@RequestBody @Validated(Category.Add.class) Category category) {
        categoryService.add(category);
        return Result.success();
    }

    @GetMapping
    public Result<List<Category>> list() {
        List<Category> cs = categoryService.list();
        return Result.success(cs);
    }

    @GetMapping("/detail")
    public Result<Category> detail(Integer id) {
       Category c =  categoryService.findById(id);
       return Result.success(c);
    }
    @PutMapping
    public Result update(@RequestBody @Validated(Category.Update.class) Category category) {
        categoryService.update(category);
        return Result.success();
    }
    @DeleteMapping
    public Result delete(Integer id) {
        categoryService.delete(id);
        return Result.success();
    }


    @GetMapping("/list")
    public Result<List<CategoryVO>> getCategoryList() {
        try {
            // 尝试从ThreadLocal获取用户信息
            Integer userId = null;
            try {
                Map<String, Object> userMap = ThreadLocalUtil.get();
                System.out.println(userMap);
                if (userMap != null) {
                    userId = (Integer) userMap.get("id");
                    System.out.println("userId: " + userId);
                }
            } catch (Exception e) {
                // 获取用户信息失败，视为无token
                userId = null;
            }

            List<CategoryVO> categoryList = categoryService.getCategoryList(userId);
            return Result.success(categoryList);
        } catch (Exception e) {
            return Result.error("获取分类列表失败");
        }
    }

}
