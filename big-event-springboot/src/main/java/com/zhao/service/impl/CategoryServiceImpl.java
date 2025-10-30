package com.zhao.service.impl;

import com.zhao.mapper.CategoryMapper;
import com.zhao.pojo.Category;
import com.zhao.pojo.CategoryVO;
import com.zhao.service.CategoryService;
import com.zhao.utils.ThreadLocalUtil;
import com.zhao.utils.UserContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public void add(Category category) {
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        //补充属性值
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        category.setCreateUser(userId);
        categoryMapper.add(category);
    }

    @Override
    public List<Category> list() {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        return categoryMapper.list(userId);
    }

    @Override
    public Category findById(Integer id) {
       Category c = categoryMapper.findById(id);
       return c;
    }

    @Override
    public void update(Category category) {
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
    }

    @Override
    public void delete(Integer id) {
        categoryMapper.delete(id);
    }




    @Override
    public List<CategoryVO> getCategoryList(Integer userId) {
        // 查询所有分类及文章数量
        List<CategoryVO> allCategories = categoryMapper.findAllWithArticleCount();

        if (userId != null) {
            // 如果有用户ID，查询该用户创建的分类ID列表
            List<Integer> userCategoryIds = categoryMapper.findUserCategoryIds(userId);

            // 标记哪些分类是当前用户创建的
            for (CategoryVO category : allCategories) {
                category.setUserCreated(userCategoryIds.contains(category.getId()));
            }
        } else {
            // 如果没有用户ID，所有分类都标记为非当前用户创建
            for (CategoryVO category : allCategories) {
                category.setUserCreated(false);
            }
        }

        return allCategories;
    }

}
