package com.zhao.service;

import com.zhao.pojo.Category;
import com.zhao.pojo.CategoryVO;

import java.util.List;

public interface CategoryService {

    //新增加分类
    void add(Category category);


    // 获取分类列表（考虑是否有token）
    List<CategoryVO> getCategoryList(Integer userId);


    //文章分类列表查询
    List<Category> list();

    //根据Id查询分类信息
    Category findById(Integer id);

    //更新文章分类
    void update(Category category);

    //删除文章分类
    void delete(Integer id);
}
