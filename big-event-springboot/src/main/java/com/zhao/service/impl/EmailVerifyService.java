package com.zhao.service.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;


/**
 * 邮箱验证服务类
 * 这个类专门负责处理邮箱验证码的发送和验证工作
 * 包括：生成验证码、发送邮件、存储验证码到Redis、验证用户输入的验证码
 */
@Service // 告诉Spring这是一个服务类，会被自动扫描并创建实例

public class EmailVerifyService {

    @Autowired // 让Spring自动注入邮件发送器，我们不需要自己创建
    private JavaMailSender mailSender;

    @Autowired // 让Spring自动注入Redis操作模板，用于操作Redis数据库
    private RedisTemplate<String, Object> redisTemplate;

    // 验证码有效期5分钟（数字5表示5个单位）
    private static final long EXPIRY_TIME = 5;
    // 时间单位是分钟（上面数字5的单位）
    private static final TimeUnit EXPIRY_UNIT = TimeUnit.MINUTES;

    // Redis中存储验证码的key前缀，用于区分不同类型的验证码
    private static final String REGISTER_PREFIX = "register:";// 注册验证码的前缀
    private static final String FORGET_PREFIX = "forget:"; // 找回密码验证码的前缀

    // 从配置文件中读取发件人邮箱地址
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        // %06d 表示生成6位数字，不足6位时前面补0
        return String.format("%06d", random.nextInt(999999));
    }

    /**
     * 发送验证码到指定邮箱
     * @param email 目标邮箱地址，验证码会发送到这个邮箱
     * @param type 验证码类型：register-用于注册, forget-用于找回密码
     * @return true-发送成功, false-发送失败
     */
    public boolean sendVerifyCode(String email, String type) {
        try {
            // 1. 生成6位随机验证码
            String code = generateCode();
            // 2. 构建Redis存储的key（根据类型和邮箱区分）
            String redisKey = buildRedisKey(email, type);

            // 3. 将验证码存储到Redis中，设置5分钟自动过期
            //    这样即使我们忘记删除，Redis也会自动清理过期数据
            redisTemplate.opsForValue().set(redisKey, code, EXPIRY_TIME, EXPIRY_UNIT);

            // 4. 准备邮件内容
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail); // 设置发件人（从配置文件读取）
            message.setTo(email); // 设置收件人
            message.setSubject(getEmailSubject(type)); // 设置邮件主题
            message.setText(getEmailContent(code, type)); // 设置邮件正文

            // 5. 发送邮件
            mailSender.send(message);
            return true; // 发送成功
        } catch (Exception e) {
            e.printStackTrace(); // 打印错误信息（实际项目中应该用日志记录）
            return false; // 发送失败
        }
    }

    /**
     * 验证用户输入的验证码是否正确
     * @param email 用户邮箱地址
     * @param code 用户输入的验证码
     * @param type 验证码类型
     * @return true-验证成功, false-验证失败
     */
    public boolean verifyCode(String email, String code, String type) {
        // 1. 构建Redis key（与发送验证码时用的key相同）
        String redisKey = buildRedisKey(email, type);
        // 2. 从Redis中获取之前存储的验证码
        String storedCode = (String) redisTemplate.opsForValue().get(redisKey);

        // 3. 检查验证码是否存在（可能已过期或被删除）
        if (storedCode == null) {
            return false; // 验证码不存在或已过期
        }

        // 4. 比较用户输入的验证码和Redis中存储的是否一致
        if (storedCode.equals(code)) {
            // 验证成功，删除Redis中的验证码
            redisTemplate.delete(redisKey);
            return true;
        }
        // 验证码不匹配
        return false;
    }

    /**
     * 构建Redis中存储验证码的key
     * 格式：类型前缀 + 邮箱地址
     * 例如：register:user@qq.com 或 forget:user@qq.com
     */
    private String buildRedisKey(String email, String type) {
        if ("register".equals(type)) {
            return REGISTER_PREFIX + email; // 注册验证码的key
        } else if ("forget".equals(type)) {
            return FORGET_PREFIX + email; // 找回密码验证码的key
        }
        // 如果传入了不支持的类型，抛出异常
        throw new IllegalArgumentException("不支持的验证码类型: " + type);
    }

    /**
     * 根据验证码类型获取邮件主题
     */
    private String getEmailSubject(String type) {
        return "register".equals(type) ? "注册验证码" : "密码重置验证码";
    }


    /**
     * 根据验证码类型生成邮件内容
     */
    private String getEmailContent(String code, String type) {
        String action = "register".equals(type) ? "注册" : "重置密码";
        return String.format(
                "您的验证码是：%s，该验证码用于%s，有效期5分钟，请勿泄露给他人。",
                code, action
        );
    }
}
