package com.zhao.service.impl;

import com.zhao.mapper.AuthorApplyMapper;
import com.zhao.mapper.UserMapper;
import com.zhao.pojo.AuthorApply;
import com.zhao.pojo.User;
import com.zhao.pojo.dto.AuthorApplyDTO;
import com.zhao.service.AuthorApplyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作者申请服务实现类
 */
@Service
@Slf4j
public class AuthorApplyServiceImpl implements AuthorApplyService {
    
    @Autowired
    private AuthorApplyMapper authorApplyMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditAuthorApply(Integer applyId, Integer adminId, Integer status, String rejectReason) {
        // 1. 检查申请是否存在且状态为待审核
        AuthorApply apply = authorApplyMapper.findById(applyId);
        if (apply == null) {
            throw new RuntimeException("作者申请不存在");
        }
        
        if (apply.getStatus() != 0) { // 0表示待审核
            throw new RuntimeException("该申请已处理，无法重复操作");
        }
        
        // 2. 检查用户是否存在
        Integer userId = apply.getUserId();
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 3. 更新申请状态、审核时间和审核人
        AuthorApply updateApply = new AuthorApply();
        updateApply.setId(applyId);
        updateApply.setStatus(status);
        updateApply.setAuditTime(LocalDateTime.now());
        updateApply.setAuditUserId(adminId);
        
        // 4. 如果是拒绝，设置拒绝原因
        if (status == 2) {
            updateApply.setRejectReason(rejectReason);
        }
        
        // 5. 更新申请记录
        authorApplyMapper.updateStatus(updateApply);
        
        // 6. 如果是通过，更新用户角色为作者
        if (status == 1) {
            userMapper.updateRole(userId, 1);
            log.info("管理员{}通过了用户{}的作者申请", adminId, userId);
        } else {
            log.info("管理员{}拒绝了用户{}的作者申请，原因：{}", adminId, userId, rejectReason);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApply(AuthorApply authorApply) {
        // 1. 检查用户是否已提交过申请
        AuthorApply existingApply = authorApplyMapper.findByUserId(authorApply.getUserId());
        if (existingApply != null) {
            throw new RuntimeException("您已经提交过作者申请，请等待审核");
        }
        
        // 2. 检查用户是否存在
        User user = userMapper.findById(authorApply.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 3. 检查用户角色 - 管理员(role=0)不允许申请为作者
        if (user.getRole() == 0) {
            throw new RuntimeException("管理员账户不允许申请为作者");
        }
        
        // 4. 检查用户是否已经是作者
        if (user.getRole() == 1) { // 1表示作者
            throw new RuntimeException("您已经是作者身份，无需再次申请");
        }
        
        // 5. 设置申请信息
        authorApply.setStatus(0); // 0=待审核
        authorApply.setCreateTime(LocalDateTime.now());
        
        // 6. 加密身份证号（简单加密处理，实际项目中应使用更安全的加密方式）
        String encryptedIdCard = encryptIdCard(authorApply.getIdCard());
        authorApply.setIdCard(encryptedIdCard);
        
        // 7. 保存申请记录
        authorApplyMapper.insert(authorApply);
        log.info("用户 {} 提交了作者申请", authorApply.getUserId());
    }
    
    @Override
    public AuthorApply getApplyStatus(Integer userId) {
        AuthorApply apply = authorApplyMapper.findByUserId(userId);
        if (apply != null) {
            // 返回时脱敏身份证号，只显示前4位和后4位
            String maskedIdCard = maskIdCard(apply.getIdCard());
            apply.setIdCard(maskedIdCard);
        }
        return apply;
    }
    
    @Override
    public Map<String, Object> getAuthorApplyList(Integer page, Integer pageSize, Integer status) {
        // 1. 参数校验和默认值处理
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }
        
        // 2. 计算分页起始索引
        Integer start = (page - 1) * pageSize;
        
        // 3. 查询申请列表
        List<AuthorApplyDTO> applyList = authorApplyMapper.getAuthorApplyList(status, start, pageSize);
        
        // 4. 对身份证号进行脱敏处理
        for (AuthorApplyDTO apply : applyList) {
            apply.setIdCard(maskIdCard(apply.getIdCard()));
            // 对真实姓名进行脱敏处理，只显示姓
            if (apply.getRealName() != null && apply.getRealName().length() > 1) {
                apply.setRealName(apply.getRealName().charAt(0) + "*");
            }
        }
        
        // 5. 查询总数
        Integer total = authorApplyMapper.getAuthorApplyCount(status);
        
        // 6. 封装结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", applyList);
        result.put("total", total);
        
        return result;
    }
    
    /**
     * 加密身份证号（简单加密示例）
     */
    private String encryptIdCard(String idCard) {
        // 实际项目中应使用更安全的加密方式，如AES
        return "ENCRYPTED:" + idCard;
    }
    
    /**
     * 脱敏身份证号
     */
    private String maskIdCard(String encryptedIdCard) {
        if (encryptedIdCard == null) {
            return "****";
        }
        
        // 如果是加密的身份证号，先解密
        String idCard = encryptedIdCard;
        if (encryptedIdCard.startsWith("ENCRYPTED:")) {
            idCard = encryptedIdCard.substring("ENCRYPTED:".length());
        }
        
        // 脱敏处理，只显示前6位和后4位
        if (idCard.length() < 10) {
            return "****";
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }
}