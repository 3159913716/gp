package com.zhao.service;

import com.zhao.pojo.AuthorApply;
import com.zhao.pojo.dto.AuthorApplyDTO;

import java.util.List;
import java.util.Map;

/**
 * 作者申请服务接口
 */
public interface AuthorApplyService {
    
    /**
     * 提交作者申请
     * @param authorApply 作者申请信息
     */
    void submitApply(AuthorApply authorApply);
    
    /**
     * 获取用户的申请状态
     * @param userId 用户ID
     * @return 申请状态信息
     */
    AuthorApply getApplyStatus(Integer userId);
    
    /**
     * 审核作者申请
     * @param applyId 申请ID
     * @param adminId 审核管理员ID
     * @param status 审核状态，1=通过，2=拒绝
     * @param rejectReason 拒绝原因，仅当status=2时需要
     */
    void auditAuthorApply(Integer applyId, Integer adminId, Integer status, String rejectReason);
    
    /**
     * 获取作者申请列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param status 申请状态，0=待审核/1=通过/2=拒绝，null表示查询所有状态
     * @return 包含申请列表和总数的Map
     */
    Map<String, Object> getAuthorApplyList(Integer page, Integer pageSize, Integer status);
}