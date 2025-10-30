package com.zhao.controller;

import com.zhao.pojo.Result;
import com.zhao.service.AdminOperationLogService;
import com.zhao.service.AuthorApplyService;
import com.zhao.service.StatisticsService;
import com.zhao.service.UserService;
import com.zhao.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员相关接口
 */
@RestController
@RequestMapping("/admin")
@Validated
public class AdminController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthorApplyService authorApplyService;
    
    @Autowired
    private AdminOperationLogService adminOperationLogService;
    
    @Autowired
    private StatisticsService statisticsService;
    
    /**
     * 审核作者申请
     * @param applyId 申请ID
     * @param params 包含审核状态和拒绝原因的参数
     * @param request HTTP请求对象，用于获取IP地址
     * @return 响应结果
     */
    @PutMapping("/author-applies/{id}/audit")
    public Result auditAuthorApply(@PathVariable("id") Integer applyId, 
                                  @RequestBody Map<String, Object> params,
                                  HttpServletRequest request) {
        try {
            // 1. 获取当前管理员ID
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer currentAdminId = (Integer) userMap.get("id");
            
            // 2. 获取审核状态（1=通过，2=拒绝）
            Integer status = (Integer) params.get("status");
            if (status == null || (status != 1 && status != 2)) {
                return Result.error("审核状态无效，请传入1(通过)或2(拒绝)");
            }
            
            // 3. 如果是拒绝，需要获取拒绝原因
            String rejectReason = null;
            if (status == 2) {
                rejectReason = (String) params.get("rejectReason");
                if (rejectReason == null || rejectReason.trim().isEmpty()) {
                    return Result.error("拒绝申请必须提供拒绝原因");
                }
            }
            
            // 4. 调用服务层方法处理审核逻辑
            authorApplyService.auditAuthorApply(applyId, currentAdminId, status, rejectReason);
            
            // 5. 记录管理员操作日志到数据库
            String ipAddress = request.getRemoteAddr();
            String operationContent = "管理员审核作者申请" + applyId + "，审核结果：" + 
                                     (status == 1 ? "通过" : "拒绝(" + rejectReason + ")");
            
            // 调用日志服务记录操作
            adminOperationLogService.recordLog(currentAdminId, operationContent, ipAddress);
            
            // 6. 返回成功结果
            return Result.success(status == 1 ? "申请已通过" : "申请已拒绝");
        } catch (RuntimeException e) {
            // 异常处理
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }
    

    
    /**
     * 修改用户角色
     * @param userId 用户ID
     * @param params 包含角色信息的参数
     * @param request HTTP请求对象，用于获取IP地址
     * @return 操作结果
     */
    @PutMapping("/users/{id}/role")
    public Result updateUserRole(@PathVariable("id") Integer userId, 
                                @RequestBody Map<String, Integer> params,
                                HttpServletRequest request) {
        try {
            // 1. 验证管理员权限
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer role = (Integer) userMap.get("role");
            if (role != 0) {
                return Result.error("没有权限执行此操作");
            }
            
            // 2. 检查是否尝试修改自身角色
            Integer currentAdminId = (Integer) userMap.get("id");
            if (userId.equals(currentAdminId)) {
                return Result.error("管理员不能修改自身角色");
            }
            
            Integer newRole = params.get("role");
            
            // 3. 参数校验
            if (newRole == null) {
                return Result.error("缺少必要参数");
            }
            
            // 4. 调用服务层更新用户角色
            userService.updateUserRole(userId, newRole);
            
            // 5. 如果是改为作者，检查是否有作者申请记录并更新状态
            // 注意：这里需要在AuthorApplyService中添加相应的方法实现
            
            // 6. 记录管理员操作日志到数据库
            String ipAddress = request.getRemoteAddr();
            String operationContent = "管理员将用户[" + userId + "]的角色修改为[" + newRole + "]";
            
            // 调用日志服务记录操作
            adminOperationLogService.recordLog(currentAdminId, operationContent, ipAddress);
            
            return Result.success("用户角色修改成功");
        } catch (RuntimeException e) {
            // 异常处理
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }
    

    
    /**
     * 获取作者申请列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param status 申请状态（0=待审核/1=通过/2=拒绝，不传表示查询所有）
     * @return 作者申请列表和分页信息
     */
    @GetMapping("/author-applies")
    public Result getAuthorApplies(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                  @RequestParam(required = false) Integer status) {
        try {
            // 1. 验证管理员权限
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer role = (Integer) userMap.get("role");
            if (role != 0) {
                return Result.error("没有权限执行此操作");
            }
            
            // 2. 调用服务层获取作者申请列表
            Map<String, Object> result = authorApplyService.getAuthorApplyList(page, pageSize, status);
            
            return Result.success(result);
        } catch (RuntimeException e) {
            // 异常处理
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户列表（管理员功能）
     * @param page 页码
     * @param pageSize 每页大小
     * @param role 用户角色（可选）
     * @param username 用户名搜索（可选）
     * @param status 用户状态（可选）
     * @return 用户列表及总数
     */
    @GetMapping("/users")
    public Result getUserList(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestParam(required = false) Integer role,
                             @RequestParam(required = false) String username,
                             @RequestParam(required = false) Integer status) {
        try {
            // 1. 验证管理员权限
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer currentRole = (Integer) userMap.get("role");
            if (currentRole != 0) {
                return Result.error("没有权限执行此操作");
            }
            
            // 2. 获取当前管理员ID，用于排除自己（可选）
            Integer currentAdminId = (Integer) userMap.get("id");
            
            // 3. 调用服务层获取用户列表
            Map<String, Object> result = userService.getUserList(page, pageSize, role, username, status, currentAdminId);
            
            return Result.success(result);
        } catch (RuntimeException e) {
            // 异常处理
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }



    /**
     * 获取数据统计（管理员功能）
     * @return 统计数据
     */
    @GetMapping("/statistics")
    public Result getDataStatistics() {
        try {
            // 1. 验证管理员权限
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer role = (Integer) userMap.get("role");
            if (role != 0) {
                return Result.error("没有权限访问统计数据");
            }
            
            // 2. 获取统计数据
            Map<String, Object> statistics = statisticsService.getStatistics();
            
            return Result.success(statistics);
        } catch (Exception e) {
            // 异常处理
            e.printStackTrace();
            return Result.error("获取统计数据失败");
        }
    }
    
    /**
     * 获取客户端IP地址
     * @param request HTTP请求对象
     * @return IP地址字符串
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理情况下，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }



}