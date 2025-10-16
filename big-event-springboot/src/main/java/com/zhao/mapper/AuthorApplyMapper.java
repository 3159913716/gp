package com.zhao.mapper;

import com.zhao.pojo.AuthorApply;
import com.zhao.pojo.dto.AuthorApplyDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 作者申请Mapper接口
 */
@Mapper
public interface AuthorApplyMapper {
    
    /**
     * 插入作者申请记录
     * @param authorApply 作者申请对象
     */
    void insert(AuthorApply authorApply);
    
    /**
     * 根据用户ID查询作者申请记录
     * @param userId 用户ID
     * @return 作者申请记录
     */
    AuthorApply findByUserId(@Param("userId") Integer userId);
    
    /**
     * 根据申请ID查询作者申请记录
     * @param id 申请ID
     * @return 申请记录
     */
    AuthorApply findById(@Param("id") Integer id);
    
    /**
     * 更新作者申请状态
     * @param authorApply 包含更新信息的作者申请对象
     */
    void updateStatus(AuthorApply authorApply);
    
    /**
     * 获取作者申请列表，支持分页和状态过滤
     * @param status 申请状态，0=待审核/1=通过/2=拒绝，null表示查询所有状态
     * @param start 起始索引
     * @param pageSize 每页大小
     * @return 作者申请列表
     */
    List<AuthorApplyDTO> getAuthorApplyList(@Param("status") Integer status, 
                                           @Param("start") Integer start, 
                                           @Param("pageSize") Integer pageSize);
    
    /**
     * 获取作者申请总数，支持状态过滤
     * @param status 申请状态，0=待审核/1=通过/2=拒绝，null表示查询所有状态
     * @return 申请总数
     */
    Integer getAuthorApplyCount(@Param("status") Integer status);
}