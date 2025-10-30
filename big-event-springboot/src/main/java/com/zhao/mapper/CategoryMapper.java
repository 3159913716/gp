package com.zhao.mapper;

import com.zhao.pojo.Category;
import com.zhao.pojo.CategoryVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {
    //新增分类
    void add(Category category);



    // 查询所有分类及每个分类下的文章数量
    List<CategoryVO> findAllWithArticleCount();


    //查询该用户创建的文章分类列表
    List<Category> list(Integer userId);

    // 查询指定用户创建的分类ID列表
    List<Integer> findUserCategoryIds(Integer userId);

    //根据Id查询
    Category findById(Integer id);

    //更新分类
    void update(Category category);

    //删除分类
    void delete(Integer id);
}
