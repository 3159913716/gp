package com.zhao.mapper;

import com.zhao.pojo.Article;
import com.zhao.pojo.ArticleHomeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ArticleMapper {
    //新增文章
    void add(Article article);

    List<Article> list(Integer userId, Integer categoryId, String state);

    //根据id查找文章
    Article findById(Integer id);

    //修改文章
    void update(Article article);

    //删除文章
    void delete(Integer id);

    List<ArticleHomeVO> selectHomeArticles(
            @Param("sort") String sort,
            @Param("state") String state,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    Long countHomeArticles(@Param("state") String state);

}
