package com.zhao.service;

import com.zhao.pojo.User;

import java.util.Map;

public interface UserService {

    //根据用户名查询用户
    User findByUserName(String username);
    
    //根据ID查询用户
    User findById(Integer id);
    
    //根据邮箱查询用户
    User findByEmail(String email);
    
    //根据ID更新密码
    void updatePasswordById(Integer userId, String newPassword);

    //注册
    void register(String username, String password, String email);

    //更新
    void update(User user);

    //更新头像
    void updateAvatar(String avatarUrl);

    //更新密码
    void updatePwd(String newPwd);
    
    // 修改用户角色
    void updateUserRole(Integer userId, Integer role);
    
    // 获取用户列表（分页、多条件筛选）
    Map<String, Object> getUserList(Integer page, Integer pageSize, Integer role, String username, Integer status, Integer excludeId);
    
    // 更新用户最后登录时间
    void updateLastLoginTime(Integer userId);
}
