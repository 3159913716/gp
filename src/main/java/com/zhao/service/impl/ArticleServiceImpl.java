package com.zhao.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zhao.mapper.ArticleMapper;
import com.zhao.pojo.Article;
import com.zhao.pojo.PageBean;
import com.zhao.service.ArticleService;
import com.zhao.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public void add(Article article) {
        //补充属性值
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        article.setCreateUser(userId);
        articleMapper.add(article);
    }

    @Override
    public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
        //1.创建PageBean对象
        PageBean<Article> pb = new PageBean<>();
        //2.开启分页查询 PageHelper插件
        PageHelper.startPage(pageNum, pageSize);
        //调用mapper完成查询
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        List<Article> as = articleMapper.list(userId, categoryId, state);
        //Page提供了方法，可以获取PageHelper分页查询后得到的总记录条数和当前页数据
        Page<Article> p = (Page<Article>) as;
        //把数据填充到PageBean对象中
        pb.setTotal(p.getTotal());
        pb.setItem(p.getResult());
        return pb;
    }

    @Override
    public Article findById(Integer id) {
         Article a = articleMapper.findById(id);
        return a;
    }

    @Override
    public void update(Article article) {
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.update(article);
    }

    @Override
    public void delete(Integer id) {
        articleMapper.delete(id);
    }
}
