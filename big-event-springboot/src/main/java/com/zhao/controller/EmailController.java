package com.zhao.controller;


import com.zhao.service.impl.EmailVerifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 邮箱相关
 * */
@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailVerifyService emailVerifyService;

    /**
     * 发送验证码
     */
    @PostMapping("/send-code")
    public ApiResponse sendCode(@RequestParam String email,
                                @RequestParam String type) {
        // 验证类型参数
        if (!"register".equals(type) && !"forget".equals(type)) {
            return ApiResponse.error("类型参数错误，必须是register或forget");
        }

        // 验证邮箱格式
        if (!isValidEmail(email)) {
            return ApiResponse.error("邮箱格式不正确");
        }

        boolean success = emailVerifyService.sendVerifyCode(email, type);
        return success ? ApiResponse.success("验证码发送成功") :
                ApiResponse.error("验证码发送失败，请稍后重试");
    }

    /**
     * 验证验证码
     */
    @PostMapping("/verify")
    public ApiResponse verify(@RequestParam String email,
                              @RequestParam String code,
                              @RequestParam String type) {



        // 彻底清理参数 - 去除所有空白字符
        type = type != null ? type.replaceAll("\\s+", "") : type;
        email = email != null ? email.trim() : email;
        code = code != null ? code.trim() : code;

        // 调试信息
//        System.out.println("调试 - email: " + email + ", code: " + code + ", type: " + type);


        // 参数清理和验证
        if (type == null) {
            return ApiResponse.error("类型参数不能为空");
        }


        // 验证类型参数
        if (!"register".equals(type) && !"forget".equals(type)) {
            System.out.println("类型验证失败，实际类型: " + type);
            return ApiResponse.error("类型参数错误");
        }


        // 验证邮箱
        if (email == null || email.trim().isEmpty()) {
            return ApiResponse.error("邮箱不能为空");
        }
        email = email.trim();



        // 验证验证码格式
        if (code == null || !code.matches("\\d{6}")) {
            return ApiResponse.error("验证码必须是6位数字");
        }

        boolean isValid = emailVerifyService.verifyCode(email, code, type);
        return isValid ? ApiResponse.success("验证成功") :
                ApiResponse.error("验证码错误或已过期");
    }

    /**
     * 简单的邮箱格式验证
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * 统一响应格式
     */
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ApiResponse success(String message) {
            return new ApiResponse(true, message);
        }

        public static ApiResponse error(String message) {
            return new ApiResponse(false, message);
        }

        // getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

}
