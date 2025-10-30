package com.zhao.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * 用户上下文工具类
 * 用于获取当前用户信息，支持从ThreadLocal获取或从请求头解析token
 */
@Slf4j
@Component
public class UserContextUtil {

    private static StringRedisTemplate stringRedisTemplate;

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        UserContextUtil.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 获取当前用户ID
     * 优先从ThreadLocal获取，如果没有则尝试从请求头解析token
     * @return 用户ID，如果未登录则返回null
     */
    public static Integer getCurrentUserId() {
        return getCurrentUserId(null);
    }

    /**
     * 获取当前用户ID
     * 优先从ThreadLocal获取，如果没有则尝试从请求头解析token
     * @param request HttpServletRequest对象，如果为null则从当前请求上下文获取
     * @return 用户ID，如果未登录则返回null
     */
    public static Integer getCurrentUserId(HttpServletRequest request) {
        Integer userId = null;
        try {
            // 1. 优先从ThreadLocal获取（如果拦截器已处理）
            Map<String, Object> userMap = ThreadLocalUtil.get();
            if (userMap != null) {
                userId = (Integer) userMap.get("id");
                log.debug("从ThreadLocal获取用户ID: {}", userId);
                return userId;
            }

            // 2. 如果ThreadLocal中没有用户信息，尝试从请求头解析token
            if (request == null) {
                request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            }

            String token = request.getHeader("Authorization");
            if (token != null && !token.trim().isEmpty()) {
                // 处理Bearer前缀
                if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
                    token = token.substring(7).trim();
                }

                // 尝试解析token
                if (!token.isEmpty()) {
                    userId = parseTokenFromHeader(token);
                    if (userId != null) {
                        log.debug("从token解析用户ID: {}", userId);
                    }
                }
            }
        } catch (Exception e) {
            // 用户未登录或token无效，userId保持为null
            log.debug("获取用户ID失败: {}", e.getMessage());
        }

        return userId;
    }

    /**
     * 从token字符串中解析用户ID
     * @param token token字符串
     * @return 用户ID，如果token无效则返回null
     */
    private static Integer parseTokenFromHeader(String token) {
        try {
            // 验证token是否在redis中存在且未过期
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            String redisToken = operations.get(token);
            if (redisToken != null) {
                Map<String, Object> claims = JwtUtil.parseToken(token);
                if (claims != null && claims.containsKey("id")) {
                    return (Integer) claims.get("id");
                }
            }
        } catch (Exception e) {
            log.debug("解析token失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取当前用户信息
     * @return 用户信息Map，如果未登录则返回null
     */
    public static Map<String, Object> getCurrentUserInfo() {
        try {
            return ThreadLocalUtil.get();
        } catch (Exception e) {
            log.debug("获取用户信息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查用户是否已登录
     * @return 如果用户已登录则返回true，否则返回false
     */
    public static boolean isUserLoggedIn() {
        return getCurrentUserId() != null;
    }
}