package com.zhao.mapper;

import com.zhao.pojo.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Service;

@Mapper
public interface UserMapper {

    //根据用户名查询用户
    User findByUserName(String username);

    //添加
    void add(String username, String encryptPassword);

    //更新
    void update(User user);

    //更新用户头像
    void updateAvatar(String avatarUrl, Integer id);

    //更新用户密码
    void updatePwd(String hsPassword, Integer id);
}
