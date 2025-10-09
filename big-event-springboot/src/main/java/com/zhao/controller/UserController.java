package com.zhao.controller;


import com.zhao.pojo.Result;
import com.zhao.pojo.User;
import com.zhao.service.UserService;
import com.zhao.utils.JwtUtil;
import com.zhao.utils.PasswordUtil;
import com.zhao.utils.ThreadLocalUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 注册接口
     */
    @PostMapping("/register")
    //校验参数是否符合要求
    public Result register(@Pattern(regexp = "^\\S{5,16}$") String username, @Pattern(regexp = "^\\S{5,16}$") String password) {


        //查询用户
        User u = userService.findByUserName(username);
        if (u == null) {
            //用户名没有占用
            //注册
            userService.register(username, password);
            return Result.success();
        } else {
            //用户名被占用
            return Result.error("用户名已被占用！");
        }

    }

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public Result<String> login(@Pattern(regexp = "^\\S{5,16}$") String username, @Pattern(regexp = "^\\S{5,16}$") String password) {

        //根据用户名查询用户
        User loginUser = userService.findByUserName(username);

        //判断该用户是否存在
        if (loginUser == null) {
            return Result.error("用户名或密码错误！");
        }
        //loginUser对象中的密码是密文
        if (PasswordUtil.verifyPassword(password, loginUser.getPassword())) {
            //登录成功，响应jwt令牌
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", loginUser.getId());
            claims.put("username", loginUser.getUsername());
            String token = JwtUtil.genToken(claims);
            //把token存储到redis中
            ValueOperations<String,String> operations = stringRedisTemplate.opsForValue();
            operations.set(token,token,12, TimeUnit.HOURS);
            return Result.success(token);
        }
        //判断密码是否正确
        return Result.error("用户名或密码错误！");
    }

    /**
     * 获取用户详细信息
     */
    @GetMapping("/userInfo")
    public Result<User> userInfo(/*@RequestHeader(name = "Authorization") String token*/) {
        //根据用户名查询用户
        //Map<String, Object> map = JwtUtil.parseToken(token);
        //String username = (String) map.get("username");
        Map<String, Object> map = ThreadLocalUtil.get();
        String username = (String) map.get("username");
        User user = userService.findByUserName(username);
        return Result.success(user);
    }

    /**
     * 更改用户信息(昵称,电子邮件)
     */
    @PutMapping("/update")
    public Result update(@RequestBody @Validated User user) {
        //@RequestBody 将前端传回来的json字符串转换成实体类对象
        userService.update(user);
        return Result.success();
    }
    /**
    * @RequestParam 注解可以将请求参数的值绑定到控制器方法的对应参数上
    * 更新用户头像
    * @URL 校验是否是url地址
    */
    @PatchMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam @URL String avatarUrl) {
        userService.updateAvatar(avatarUrl);
        return Result.success();
    }

    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String, String> params, @RequestHeader("Authorization") String token) {


        //1.校验参数
        String oldPwd = params.get("old_pwd");
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");
        if (!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd) || !StringUtils.hasLength(rePwd)) {
            return Result.error("缺少必要的参数!");
        }


        //调用userService 根据用户名拿到密码,再和old_pwd比对
        Map<String,Object> map = ThreadLocalUtil.get();
        String username = (String) map.get("username");
        User loginUser = userService.findByUserName(username);
        if(!PasswordUtil.verifyPassword(oldPwd, loginUser.getPassword())) {
            return Result.error("原密码不正确!");
        }

        //检验newPwd和rePwd是否一致
        if(!rePwd.equals(newPwd)) {
            return Result.error("两次填写的新密码不一致!");
        }

        //调用service完成密码更新
        userService.updatePwd(newPwd);
        //删除redis中对应的token
        ValueOperations<String,String> operations = stringRedisTemplate.opsForValue();
        operations.getOperations().delete(token);
        return Result.success();

    }
}

