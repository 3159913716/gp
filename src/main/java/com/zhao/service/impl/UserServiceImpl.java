package com.zhao.service.impl;

import com.zhao.mapper.UserMapper;
import com.zhao.pojo.User;
import com.zhao.service.UserService;
import com.zhao.utils.PasswordUtil;
import com.zhao.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User findByUserName(String username) {
//        User u = userMapper.findByUserName(username);
        return userMapper.findByUserName(username);
    }

    @Override
    public void register(String username, String password) {
        //密码加密
        String encryptPassword = PasswordUtil.encryptPassword(password);
        //添加
        userMapper.add(username, encryptPassword);
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
        userMapper.updateAvatar(avatarUrl, id);
    }

    @Override
    public void updatePwd(String newPwd) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer id = (Integer) map.get("id");
        userMapper.updatePwd(PasswordUtil.encryptPassword(newPwd), id);
    }
}
