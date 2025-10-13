package com.zhao.interceptors;

import com.zhao.utils.JwtUtil;
import com.zhao.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

//拦截器
@Component
public class LoginInterceptors implements HandlerInterceptor {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //令牌验证
        String token = request.getHeader("Authorization");
        try {
            // 检查token是否为null
            if (token == null || token.trim().isEmpty()) {
                response.setStatus(401);
                response.setContentType("application/json; charset=UTF-8");
                response.getWriter().write("{\"code\": 0, \"message\": \"未提供认证令牌\", \"data\": null}");
                return false;
            }
            
            // 处理Bearer前缀
            if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
                token = token.substring(7).trim();
            }
            
            // 确保token不为空
            if (token.isEmpty()) {
                response.setStatus(401);
                response.setContentType("application/json; charset=UTF-8");
                response.getWriter().write("{\"code\": 0, \"message\": \"认证令牌格式错误\", \"data\": null}");
                return false;
            }
            
            //从redis中获取相同的token
            ValueOperations<String,String> operations = stringRedisTemplate.opsForValue();
            String redisToken = operations.get(token);
            if (redisToken == null) {
                //token已经失效了
                response.setStatus(401);
                response.setContentType("application/json; charset=UTF-8");
                response.getWriter().write("{\"code\": 0, \"message\": \"认证令牌已过期\", \"data\": null}");
                return false;
            }
            Map<String, Object> claims = JwtUtil.parseToken(token);
            //把业务数据存放发到ThreadLocal
            ThreadLocalUtil.set(claims);
            //验证通过,放行!!
            return true;
        } catch (Exception e) {
            //http响应状态码为401
            response.setStatus(401);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"code\": 0, \"message\": \"认证失败：" + e.getMessage() +"\", \"data\": null}");
            //验证不通过,不放行
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清空ThreadLocal中的数据
        ThreadLocalUtil.remove();
    }
}
