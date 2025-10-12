package com.zhao.service.impl;

import com.zhao.mapper.ArticleCollectMapper;
import com.zhao.pojo.ArticleCollectionVO;
import com.zhao.pojo.PageBean;
import com.zhao.service.UserCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserCollectionServiceImpl implements UserCollectionService {

    @Autowired
    private ArticleCollectMapper articleCollectMapper;

    @Override
    public PageBean<ArticleCollectionVO> getUserCollections(Integer userId, Integer page, Integer pageSize) {
        // 计算起始索引
        int start = (page - 1) * pageSize;
        
        // 查询收藏列表
        List<ArticleCollectionVO> collectionList = articleCollectMapper.findUserCollections(userId, start, pageSize);
        
        // 查询总收藏数
        int total = articleCollectMapper.countUserCollections(userId);
        
        // 构建并返回PageBean
        PageBean<ArticleCollectionVO> pageBean = new PageBean<>();
        pageBean.setItem(collectionList);
        pageBean.setTotal((long) total);
        
        return pageBean;
    }
}