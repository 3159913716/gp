package com.zhao.service.impl;

import com.zhao.mapper.UserMapper;
import com.zhao.pojo.User;
import com.zhao.service.UserService;
import com.zhao.utils.PasswordUtil;
import com.zhao.utils.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User findByUserName(String username) {
//        User u = userMapper.findByUserName(username);
        return userMapper.findByUserName(username);
    }
    
    @Override
    public User findById(Integer id) {
        return userMapper.findById(id);
    }
    
    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }
    
    @Override
    public User findByPhone(String phone) {
        return userMapper.findByPhone(phone);
    }
    
    @Override
    public void updatePasswordById(Integer userId, String newPassword) {
        // 密码加密
        String encryptPassword = PasswordUtil.encryptPassword(newPassword);
        // 更新密码
        userMapper.updatePasswordById(encryptPassword, userId);
    }

    @Override
    public void register(String username, String password, String email) {
        //密码加密
        String encryptPassword = PasswordUtil.encryptPassword(password);
        //添加
        userMapper.add(username, encryptPassword, email);
    }
    
    @Override
    public void registerByPhone(String username, String password, String phone) {
        //密码加密
        String encryptPassword = PasswordUtil.encryptPassword(password);
        //添加
        userMapper.addByPhone(username, encryptPassword, phone);
    }

    @Override
    public void update(User user) {
        user.setUpdateTime(LocalDateTime.now());
        userMapper.update(user);
    }

    @Override
    public void updateAvatar(String avatarUrl) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer id = (Integer) map.get("id");
        log.info("updateAvatar中使用的id为{}",id);
        userMapper.updateAvatar(avatarUrl, id);
    }

    @Override
    public void updatePwd(String newPwd) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer id = (Integer) map.get("id");
        userMapper.updatePwd(PasswordUtil.encryptPassword(newPwd), id);
    }
    
    @Override
    public void updateUserRole(Integer userId, Integer role) {
        // 验证角色值是否有效（0-管理员，1-作者，2-普通用户）
        if (role < 0 || role > 2) {
            throw new RuntimeException("无效的角色值");
        }
        // 检查用户是否存在
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 更新用户角色
        userMapper.updateRole(userId, role);
    }
    
    @Override
    public void updateLastLoginTime(Integer userId) {
        userMapper.updateLastLoginTime(userId);
    }
    
    @Override
    public Map<String, Object> getUserList(Integer page, Integer pageSize, Integer role, String username, Integer status, Integer excludeId) {
        // 记录查询参数，特别是status参数
        log.info("查询用户列表，参数：page={}, pageSize={}, role={}, username={}, status={}, excludeId={}", 
                page, pageSize, role, username, status, excludeId);
        
        // 参数验证和默认值处理
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        // 限制最大分页大小
        if (pageSize > 100) {
            pageSize = 100;
        }
        
        // 计算起始索引
        int start = (page - 1) * pageSize;
        
        // 查询用户列表
        List<User> userList = userMapper.getUserList(start, pageSize, role, username, status, excludeId);
        
        // 查询用户总数
        Integer total = userMapper.getUserCount(role, username, status, excludeId);
        
        // 记录查询结果
        log.info("查询用户列表结果：找到 {} 条记录，总数为 {}", userList.size(), total);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", userList);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        return result;
    }
}
