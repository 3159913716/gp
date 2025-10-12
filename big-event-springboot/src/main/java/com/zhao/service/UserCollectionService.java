package com.zhao.service;

import com.zhao.pojo.ArticleCollectionVO;
import com.zhao.pojo.PageBean;

public interface UserCollectionService {
    /**
     * 获取用户的收藏列表
     * @param userId 用户ID
     * @param page 当前页码
     * @param pageSize 每页条数
     * @return 分页后的收藏列表
     */
    PageBean<ArticleCollectionVO> getUserCollections(Integer userId, Integer page, Integer pageSize);
}