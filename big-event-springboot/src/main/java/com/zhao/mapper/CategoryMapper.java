package com.zhao.mapper;

import com.zhao.pojo.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {
    //新增分类
    void add(Category category);

    //查询该用户创建的文章分类列表
    List<Category> list(Integer userId);

    //根据Id查询
    Category findById(Integer id);

    //更新分类
    void update(Category category);

    //删除分类
    void delete(Integer id);
}
