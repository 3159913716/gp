package com.zhao.controller;

import com.zhao.pojo.Article;
import com.zhao.pojo.ArticleCollectionVO;
import com.zhao.pojo.AuthorApply;
import com.zhao.pojo.PageBean;
import com.zhao.pojo.Result;
import com.zhao.pojo.User;
import com.zhao.service.ArticleService;
import com.zhao.service.AuthorApplyService;
import com.zhao.service.UserCollectionService;
import com.zhao.service.UserFollowService;
import com.zhao.service.UserService;
import com.zhao.utils.JwtUtil;
import com.zhao.utils.PasswordUtil;
import com.zhao.utils.ThreadLocalUtil;
import jakarta.validation.Valid;
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

    /**
     * 注册接口
     */
    @PostMapping("/register")
    //校验参数是否符合要求
    public Result register(@Pattern(regexp = "^\\S{5,17}$") String username, @Pattern(regexp = "^\\S{5,17}$") String password) {


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

    /**
     * 更新用户密码
     * @param params 包含旧密码、新密码和确认密码的参数
     * @param token 用户认证令牌
     * @return 操作结果
     */
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
        // 针对身份证号格式错误返回友好提示
        return Result.error("身份证号格式不正确，请输入有效的18位身份证号");
    }
    
    /**
     * 提交作者申请
     * @param apply 申请信息
     * @return 操作结果
     */
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
     * 获取当前用户的文章列表
     * 需要作者权限
     * @param page 当前页码，默认1
     * @param pageSize 每页大小，默认10
     * @param state 文章状态（可选，草稿或已发布）
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

