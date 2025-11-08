package com.zhao.service.impl;

import com.zhao.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Spug短信服务实现类
 * 负责处理短信验证码的发送和验证逻辑
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final RestTemplate restTemplate = new RestTemplate();
    
    // Spug短信服务配置
    @Value("${spug.sms.api-url}")
    private String spugSmsApiUrl;
    
    @Value("${spug.sms.api-key}")
    private String spugSmsApiKey;
    
    @Value("${spug.sms.template-id}")
    private String spugSmsTemplateId;
    
    @Value("${spug.sms.code-expiry:5}")
    private int codeExpiryMinutes;
    
    // 已经在上面声明了相关配置字段
    
    // 使用配置文件中的有效期
    
    // Redis中存储验证码的key前缀
    private static final String REGISTER_PREFIX = "sms:register:";
    private static final String LOGIN_PREFIX = "sms:login:";
    private static final String RESET_PREFIX = "sms:reset:";
    // Redis中存储发送频率限制的key前缀
    private static final String SMS_RATE_LIMIT_PREFIX = "sms:rate:limit:";
    
    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }
    
    /**
     * 构建Redis存储验证码的key
     */
    private String buildRedisKey(String targets, String type) {
        switch (type) {
            case "register":
                return REGISTER_PREFIX + targets;
            case "login":
                return LOGIN_PREFIX + targets;
            case "reset":
                return RESET_PREFIX + targets;
            default:
                throw new IllegalArgumentException("不支持的验证码类型: " + type);
        }
    }
    
    @Override
    public boolean sendVerifyCode(String targets, String type) {
        try {
            // 1. 验证手机号格式（简单验证，实际项目可使用更严格的正则）
            if (targets == null || targets.length() != 11 || !targets.matches("^1[3-9]\\d{9}$")) {
                return false;
            }
            
            // 2. 验证类型参数
            if (!"register".equals(type) && !"login".equals(type) && !"reset".equals(type)) {
                return false;
            }
            
            // 3. 检查频率限制 - 同一手机号1分钟内只能发送一次
            String rateLimitKey = SMS_RATE_LIMIT_PREFIX + targets;
            Boolean exists = redisTemplate.hasKey(rateLimitKey);
            if (exists != null && exists) {
                log.warn("手机号 {} 发送验证码过于频繁", targets);
                return false;
            }
            
            // 4. 生成6位验证码
            String code = generateCode();
            
            // 5. 构建Redis Key并存储验证码
            String redisKey = buildRedisKey(targets, type);
            redisTemplate.opsForValue().set(redisKey, code, codeExpiryMinutes, TimeUnit.MINUTES);
            
            // 设置频率限制 - 1分钟内不允许再次发送
            redisTemplate.opsForValue().set(rateLimitKey, "1", 1, TimeUnit.MINUTES);
            
            log.info("已发送短信验证码到 {}，类型：{}", targets, type);
            
            // 5. 调用Spug短信服务API发送验证码
            // 这里预留API调用，实际使用时需要根据Spug的API文档调整参数
            if (sendSmsToSpug(targets, code, type)) {
                return true;
            }
            
            // 发送失败，删除Redis中的验证码和频率限制记录
            redisTemplate.delete(redisKey);
            redisTemplate.delete(rateLimitKey);
            return false;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean verifyCode(String targets, String code, String type) {
        try {
            // 1. 验证参数
            if (targets == null || code == null || type == null || !code.matches("\\d{6}")) {
                return false;
            }
            
            // 2. 构建Redis Key并获取存储的验证码
            String redisKey = buildRedisKey(targets, type);
            String storedCode = (String) redisTemplate.opsForValue().get(redisKey);
            
            // 3. 检查验证码是否存在（已过期或被删除）
            if (storedCode == null) {
                return false;
            }
            
            // 4. 验证成功，删除Redis中的验证码
            if (storedCode.equals(code)) {
                redisTemplate.delete(redisKey);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 调用Spug短信服务API发送短信
     * 根据Spug短信接口要求，使用name、code和time三个参数
     */
    private boolean sendSmsToSpug(String targets, String code, String type) {
        try {
            // 如果配置了Spug短信API URL，则调用API
            if (spugSmsApiUrl != null && !spugSmsApiUrl.isEmpty()) {
                // 构建请求参数 - 按照Spug短信接口要求使用name、code、time三个参数
                Map<String, Object> params = new HashMap<>();
                params.put("name", "big-event"); // 可以根据实际需求设置名称
                params.put("code", code); // 验证码
                params.put("time", codeExpiryMinutes); // 有效期（分钟）
                params.put("targets", targets); // 使用targets参数替代phone用于实际发送
                
                // 添加API密钥到请求头
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + spugSmsApiKey);
                
                try {
                    // 调用Spug短信服务API发送短信
                    // 实际使用时，这里应该使用restTemplate发送HTTP请求到spugSmsApiUrl
                    log.info("调用Spug短信服务API: {}, 参数: {}", spugSmsApiUrl, params);
                    
                    // 示例调用方式（实际项目中取消注释）
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.setAll(headers);
                    HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(params, httpHeaders);
                    ResponseEntity<Map> response = restTemplate.postForEntity(spugSmsApiUrl, requestEntity, Map.class);

                    return response.getStatusCode().is2xxSuccessful();
                } catch (Exception e) {
                    log.error("发送短信失败: {}", e.getMessage());
                    throw new RuntimeException("短信发送失败，请稍后重试");
                }
            }
            
            // 为了方便开发测试，暂时返回成功
            // 实际项目中应该注释掉这行，使用真实的API调用
            return true;
        } catch (Exception e) {
            log.error("发送短信异常: {}", e.getMessage());
            return false;
        }
    }
}