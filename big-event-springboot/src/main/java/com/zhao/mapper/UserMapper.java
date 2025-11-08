package com.zhao.mapper;

import com.zhao.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserMapper {

    //根据用户名查询用户
    User findByUserName(String username);

    //根据ID查询用户
    User findById(@Param("id") Integer id);
    
    //根据邮箱查询用户
    User findByEmail(String email);
    
    //根据ID更新密码
    void updatePasswordById(@Param("encryptPassword") String encryptPassword, @Param("userId") Integer userId);

    //添加
    void add(String username, String encryptPassword, String email);

    //更新
    void update(User user);

    //更新用户头像
    void updateAvatar(String avatarUrl, Integer id);

    //更新用户密码
    void updatePwd(String hsPassword, Integer id);
    
    // 更新用户角色
    void updateRole(@Param("id") Integer userId, @Param("role") Integer role);
    
    // 获取用户列表（支持分页、角色、用户名、状态筛选）
    List<User> getUserList(@Param("start") Integer start, @Param("pageSize") Integer pageSize, 
                          @Param("role") Integer role, @Param("username") String username, 
                          @Param("status") Integer status, @Param("excludeId") Integer excludeId);
    
    // 获取用户总数（支持角色、用户名、状态筛选）
    Integer getUserCount(@Param("role") Integer role, @Param("username") String username, 
                         @Param("status") Integer status, @Param("excludeId") Integer excludeId);
    
    // 获取用户总数
    Integer getTotalUsers();
    
    // 获取今日新增用户数
    Integer getTodayNewUsers(@Param("todayStart") LocalDateTime todayStart);
    
    // 获取日活跃用户数
    Integer getDailyActiveUsers(@Param("todayStart") LocalDateTime todayStart);
    
    // 根据日期范围获取新增用户数
    Integer getNewUsersByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // 更新用户的更新时间
    void updateLastLoginTime(Integer id);
}
