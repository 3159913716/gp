package com.zhao.controller;

import com.zhao.pojo.Article;
import com.zhao.pojo.ArticleCollectionVO;
import com.zhao.pojo.AuthorApply;
import com.zhao.pojo.PageBean;
import com.zhao.pojo.Result;
import com.zhao.pojo.User;
import com.zhao.service.ArticleService;
import com.zhao.service.AuthorApplyService;
import com.zhao.service.impl.EmailVerifyService;
import com.zhao.service.SmsService;
import com.zhao.service.UserCollectionService;
import com.zhao.service.UserFollowService;
import com.zhao.service.UserService;
import com.zhao.utils.JwtUtil;
import com.zhao.utils.PasswordUtil;
import com.zhao.utils.ThreadLocalUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
    @Autowired
    private UserCollectionService userCollectionService;
    @Autowired
    private UserFollowService userFollowService;
    @Autowired
    private AuthorApplyService authorApplyService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private EmailVerifyService emailVerifyService;
    
    @Autowired
    private SmsService smsService;

    /**
     * 忘记密码 - 根据邮箱获取用户信息（用于验证邮箱是否存在）
     */
    @PostMapping("/find-by-email")
    public Result<User> findUserByEmail(@RequestParam String email) {
        // 验证邮箱格式
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return Result.error("邮箱格式不正确");
        }
        
        // 调用服务层根据邮箱查找用户
        User user = userService.findByEmail(email);
        if (user == null) {
            return Result.error("该邮箱未注册");
        }
        
        // 返回用户信息（不包含敏感信息）
        User responseUser = new User();
        responseUser.setId(user.getId());
        responseUser.setUsername(user.getUsername());
        responseUser.setEmail(user.getEmail());
        responseUser.setNickname(user.getNickname());
        
        return Result.success(responseUser);
    }
    
    /**
     * 忘记密码 - 发送验证码
     */
    @PostMapping("/send-forget-code")
    public Result sendForgetCode(@RequestParam Integer userId, @RequestParam String email) {
        // 验证邮箱格式
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return Result.error("邮箱格式不正确");
        }
        
        // 验证用户ID和邮箱是否匹配
        User user = userService.findById(userId);
        if (user == null || !email.equals(user.getEmail())) {
            return Result.error("用户ID与邮箱不匹配");
        }
        
        // 发送验证码
        boolean success = emailVerifyService.sendVerifyCode(email, "forget");
        if (success) {
            return Result.success("验证码已发送到您的邮箱，有效期5分钟");
        } else {
            return Result.error("验证码发送失败，请稍后重试");
        }
    }
    
    /**
     * 忘记密码 - 重置密码
     */
    @PostMapping("/reset-pwd")
    public Result resetPassword(@RequestBody Map<String, String> params) {
        try {
            // 获取参数
            String userIdStr = params.get("userId");
            String email = params.get("email");
            String code = params.get("code");
            String newPwd = params.get("newPwd");
            String rePwd = params.get("rePwd");
            
            // 参数校验
            if (!StringUtils.hasLength(userIdStr) || !StringUtils.hasLength(email) || 
                !StringUtils.hasLength(code) || !StringUtils.hasLength(newPwd) || !StringUtils.hasLength(rePwd)) {
                return Result.error("缺少必要的参数");
            }
            
            Integer userId;
            try {
                userId = Integer.parseInt(userIdStr);
            } catch (NumberFormatException e) {
                return Result.error("用户ID格式错误");
            }
            
            // 验证邮箱格式
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return Result.error("邮箱格式不正确");
            }
            
            // 验证验证码格式
            if (!code.matches("\\d{6}")) {
                return Result.error("验证码必须是6位数字");
            }
            
            // 验证密码格式
            if (!newPwd.matches("^\\S{5,17}$")) {
                return Result.error("新密码必须是5-17位非空字符");
            }
            
            // 验证两次密码是否一致
            if (!newPwd.equals(rePwd)) {
                return Result.error("两次输入的密码不一致");
            }
            
            // 验证用户ID和邮箱是否匹配
            User user = userService.findById(userId);
            if (user == null || !email.equals(user.getEmail())) {
                return Result.error("用户ID与邮箱不匹配");
            }
            
            // 验证验证码
            boolean isValid = emailVerifyService.verifyCode(email, code, "forget");
            if (!isValid) {
                return Result.error("验证码错误或已过期");
            }
            
            // 更新密码
            userService.updatePasswordById(userId, newPwd);
            
            return Result.success("密码重置成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("密码重置失败，请稍后重试");
        }
    }
    
    /**
     * 注册接口（邮箱方式）
     */
    @PostMapping("/register")
    //校验参数是否符合要求
    public Result register(@Pattern(regexp = "^\\S{5,17}$") String username, 
                          @Pattern(regexp = "^\\S{5,17}$") String password,
                          @Email String email) {

        //查询用户名是否被占用
        User u = userService.findByUserName(username);
        if (u != null) {
            return Result.error("用户名已被占用！");
        }
        
        //查询邮箱是否被占用
        User emailUser = userService.findByEmail(email);
        if (emailUser != null) {
            return Result.error("邮箱已被注册！");
        }
        
        //注册
        userService.register(username, password, email);
        return Result.success();
    }
    
    /**
     * 注册接口（手机号方式）
     */
    @PostMapping("/register-by-phone")
    public Result registerByPhone(@Pattern(regexp = "^\\S{5,17}$") String username, 
                                 @Pattern(regexp = "^\\S{5,17}$") String password,
                                 @Pattern(regexp = "^1[3-9]\\d{9}$") String phone,
                                 @Pattern(regexp = "^\\d{6}$") String code) {
        
        // 查询用户名是否被占用
        User u = userService.findByUserName(username);
        if (u != null) {
            return Result.error("用户名已被占用！");
        }
        
        // 查询手机号是否被占用
        User phoneUser = userService.findByPhone(phone);
        if (phoneUser != null) {
            return Result.error("手机号已被注册！");
        }
        
        // 验证短信验证码
        boolean isValid = smsService.verifyCode(phone, code, "register");
        if (!isValid) {
            return Result.error("验证码错误或已过期");
        }
        
        // 注册
        userService.registerByPhone(username, password, phone);
        return Result.success("注册成功");
    }

    /**
     * 登录接口（用户名密码方式）
     */
    @PostMapping("/login")
    public Result<String> login(@Pattern(regexp = "^\\S{5,17}$") String username, @Pattern(regexp = "^\\S{5,17}$") String password) {

        //根据用户名查询用户
        User loginUser = userService.findByUserName(username);

        //判断该用户是否存在
        if (loginUser == null) {
            return Result.error("用户名或密码错误！");
        }
        
        // 检查用户状态，如果被禁用则不允许登录
        if (loginUser.getStatus() != null && loginUser.getStatus() == 1) {
            return Result.error("账号已被禁用，请联系管理员！");
        }
        
        //loginUser对象中的密码是密文
        if (PasswordUtil.verifyPassword(password, loginUser.getPassword())) {
            // 登录成功后更新用户的最后登录时间（update_time字段）
            userService.updateLastLoginTime(loginUser.getId());
            
            //登录成功，响应jwt令牌
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", loginUser.getId());
            claims.put("username", loginUser.getUsername());
            claims.put("role", loginUser.getRole()); // 将角色信息放入JWT
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
     * 登录接口（手机号验证码方式）
     */
    @PostMapping("/login-by-phone")
    public Result<String> loginByPhone(@Pattern(regexp = "^1[3-9]\\d{9}$") String phone, 
                                      @Pattern(regexp = "^\\d{6}$") String code) {
        
        // 查询用户是否存在
        User loginUser = userService.findByPhone(phone);
        if (loginUser == null) {
            return Result.error("该手机号未注册");
        }
        
        // 检查用户状态，如果被禁用则不允许登录
        if (loginUser.getStatus() != null && loginUser.getStatus() == 1) {
            return Result.error("账号已被禁用，请联系管理员！");
        }
        
        // 验证短信验证码
        boolean isValid = smsService.verifyCode(phone, code, "login");
        if (!isValid) {
            return Result.error("验证码错误或已过期");
        }
        
        // 登录成功后更新用户的最后登录时间
        userService.updateLastLoginTime(loginUser.getId());
        
        // 登录成功，响应jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", loginUser.getId());
        claims.put("username", loginUser.getUsername());
        claims.put("role", loginUser.getRole());
        String token = JwtUtil.genToken(claims);
        // 把token存储到redis中
        ValueOperations<String,String> operations = stringRedisTemplate.opsForValue();
        operations.set(token, token, 12, TimeUnit.HOURS);
        
        return Result.success(token);
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
    public Result<Void> update(@RequestBody @Validated User user) {
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
    
    /**
     * 获取我的收藏列表
     * @param page 当前页码
     * @param pageSize 每页条数
     * @return 收藏列表
     */
    @GetMapping("/collections")
    public Result<Map<String, Object>> getUserCollections(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            // 从ThreadLocal中获取当前用户ID
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer userId = (Integer) userMap.get("id");
            
            // 调用服务层获取收藏列表
            PageBean<ArticleCollectionVO> result = userCollectionService.getUserCollections(userId, page, pageSize);
            
            // 构建返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("list", result.getItem());
            response.put("total", result.getTotal());
            response.put("page", page);
            response.put("pageSize", pageSize);
            
            return Result.success(response);
        } catch (Exception e) {
            // 异常处理
            e.printStackTrace();
            return Result.error("获取收藏列表失败");
        }
    }
    
    /**
     * 关注/取消关注用户
     * @param followedId 被关注用户的ID
     * @return 操作结果
     */
    @PostMapping("/{id}/follow")
    public Result<Map<String, Boolean>> toggleFollow(@PathVariable("id") Integer followedId) {
        try {
            // 从ThreadLocal中获取当前登录用户的ID
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer followerId = (Integer) userMap.get("id");
            
            // 调用服务层执行关注/取消关注操作
            Map<String, Boolean> result = userFollowService.toggleFollow(followerId, followedId);
            
            // 返回操作结果
            return Result.success(result);
        } catch (RuntimeException e) {
            // 异常处理
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户关注列表
     * @return 关注列表
     */
    @GetMapping("/following")
    public Result<List<Map<String, Object>>> getFollowingList() {
        try {
            // 从ThreadLocal中获取当前登录用户的ID
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer userId = (Integer) userMap.get("id");
            
            // 调用服务层获取关注列表
            List<Map<String, Object>> followingList = userFollowService.getFollowingList(userId);
            
            return Result.success(followingList);
        } catch (Exception e) {
            // 异常处理
            e.printStackTrace();
            return Result.error("获取关注列表失败");
        }
    }
    
    /**
     * 获取用户粉丝列表
     * @return 粉丝列表
     */
    @GetMapping("/followers")
    public Result<List<Map<String, Object>>> getFollowersList() {
        try {
            // 从ThreadLocal中获取当前登录用户的ID
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer userId = (Integer) userMap.get("id");
            
            // 调用服务层获取粉丝列表
            List<Map<String, Object>> followersList = userFollowService.getFollowersList(userId);
            
            return Result.success(followersList);
        } catch (Exception e) {
            // 异常处理
            e.printStackTrace();
            return Result.error("获取粉丝列表失败");
        }
    }
    
    /**
     * 提交作者申请
     * @param apply 申请信息
     * @return 操作结果
     */
    /**
     * 处理方法参数验证异常
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public Result handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        // 获取实际的验证错误信息
        String errorMessage = ex.getBindingResult().getFieldError().getDefaultMessage();
        return Result.error(errorMessage);
    }
    
    @PostMapping("/author-apply")
    public Result submitAuthorApply(@RequestBody @Valid AuthorApply apply) {
        try {
            // 从ThreadLocal中获取当前登录用户的ID
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer userId = (Integer) userMap.get("id");
            
            // 设置申请人ID
            apply.setUserId(userId);
            
            // 调用服务层提交申请
            authorApplyService.submitApply(apply);
            
            return Result.success("申请提交成功，请等待审核");
        } catch (Exception e) {
            // 异常处理
            e.printStackTrace();
            return Result.error(e.getMessage() != null ? e.getMessage() : "处理失败");
        }
    }
    
    /**
     * 获取作者申请状态
     * @return 申请状态信息
     */
    @GetMapping("/author-apply/status")
    public Result<AuthorApply> getAuthorApplyStatus() {
        try {
            // 从ThreadLocal中获取当前登录用户的ID
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer userId = (Integer) userMap.get("id");
            
            // 调用服务层获取申请状态
            AuthorApply applyStatus = authorApplyService.getApplyStatus(userId);
            
            return Result.success(applyStatus);
        } catch (Exception e) {
            // 异常处理
            e.printStackTrace();
            return Result.error("获取申请状态失败");
        }
    }
    
    /**
     * 获取我的文章列表
     * 需要作者权限
     * @param page 当前页码，默认1
     * @param pageSize 每页大小，默认10
     * @param state 文章状态（可选）
     * @return 包含文章列表和总数的分页数据
     */
    @GetMapping("/articles")
    public Result<Map<String, Object>> getUserArticles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String state) {
        try {
            // 验证作者权限
            Map<String, Object> userMap = ThreadLocalUtil.get();
            Integer role = (Integer) userMap.get("role");
            if (role != 1) {
                return Result.error("需要作者权限");
            }
            
            // 参数容错处理
            // 页码容错：页码小于1时自动设为1
            if (page < 1) {
                page = 1;
            }
            
            // 页大小容错：页大小小于1时自动设为10，大于50时自动设为50
            if (pageSize < 1) {
                pageSize = 10;
            } else if (pageSize > 50) {
                pageSize = 50;
            }
            
            // 状态参数校验：如果提供了状态参数，则必须是有效状态
            if (state != null && !state.isEmpty()) {
                // 验证状态参数是否有效
                // 有效状态为"草稿"和"已发布"
                if (!"草稿".equals(state) && !"已发布".equals(state)) {
                    return Result.error("状态参数错误，可选值：草稿, 已发布");
                }
            }
            
            // 调用服务层获取用户文章列表
            PageBean<Article> result = articleService.getUserArticles(page, pageSize, state);
            
            // 构建返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("list", result.getItem());
            response.put("total", result.getTotal());
            response.put("page", page);
            response.put("pageSize", pageSize);
            
            return Result.success(response);
        } catch (Exception e) {
            // 异常处理
            e.printStackTrace();
            return Result.error("获取文章列表失败");
        }
    }
}

