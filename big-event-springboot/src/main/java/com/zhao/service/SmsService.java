package com.zhao.service;

/**
 * 短信服务接口
 * 用于处理与Spug短信服务的交互
 */
public interface SmsService {

    /**
     * 发送短信验证码
     * @param targets 目标手机号
     * @param type 验证码类型（如register-注册, login-登录, reset-重置密码等）
     * @return 发送结果，true表示成功，false表示失败
     */
    boolean sendVerifyCode(String targets, String type);
    
    /**
     * 验证短信验证码
     * @param targets 目标手机号
     * @param code 用户输入的验证码
     * @param type 验证码类型
     * @return 验证结果，true表示验证成功，false表示验证失败或验证码已过期
     */
    boolean verifyCode(String targets, String code, String type);
}