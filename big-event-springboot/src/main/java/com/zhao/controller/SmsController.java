package com.zhao.controller;

import com.zhao.service.SmsService;
import com.zhao.pojo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 短信相关控制器
 * 提供短信验证码的发送和验证接口
 */
@RestController
@RequestMapping("/api/sms")
public class SmsController {

    @Autowired
    private SmsService smsService;

    /**
     * 发送短信验证码
     * @param targets 目标手机号
     * @param type 验证码类型：register-注册, login-登录, reset-重置密码
     * @return 操作结果
     */
    @PostMapping("/send-code")
    public Result sendCode(@RequestParam String targets, @RequestParam String type) {
        // 验证类型参数
        if (!"register".equals(type) && !"login".equals(type) && !"reset".equals(type)) {
            return Result.error("类型参数错误，必须是register、login或reset");
        }

        // 验证手机号格式
        if (!isValidPhone(targets)) {
            return Result.error("手机号格式不正确");
        }

        // 发送验证码
        boolean success = smsService.sendVerifyCode(targets, type);
        if (success) {
            return Result.success("验证码已发送，有效期5分钟");
        } else {
            // 发送失败可能是频率限制或其他原因
            return Result.error("验证码发送失败，请稍后重试（同一手机号1分钟内只能发送一次）");
        }
    }

    /**
     * 验证短信验证码
     * @param targets 目标手机号
     * @param code 用户输入的验证码
     * @param type 验证码类型
     * @return 验证结果
     */
    @PostMapping("/verify")
    public Result verifyCode(@RequestParam String targets, @RequestParam String code, @RequestParam String type) {
        // 清理和验证参数
        targets = targets != null ? targets.trim() : targets;
        code = code != null ? code.trim() : code;
        type = type != null ? type.trim() : type;

        // 验证手机号格式
        if (!isValidPhone(targets)) {
            return Result.error("手机号格式不正确");
        }

        // 验证类型参数
        if (!"register".equals(type) && !"login".equals(type) && !"reset".equals(type)) {
            return Result.error("类型参数错误");
        }

        // 验证验证码格式
        if (code == null || !code.matches("\\d{6}")) {
            return Result.error("验证码必须是6位数字");
        }

        // 调用服务验证验证码
        boolean isValid = smsService.verifyCode(targets, code, type);
        return isValid ? Result.success("验证成功") : Result.error("验证码错误或已过期");
    }

    /**
     * 验证手机号格式
     */
    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
}