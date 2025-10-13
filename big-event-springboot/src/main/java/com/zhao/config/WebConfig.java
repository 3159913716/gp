package com.zhao.config;

import com.zhao.interceptors.LoginInterceptors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//配置类,注册拦截器
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptors loginInterceptors;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //配置不应当拦截的路径
        registry.addInterceptor(loginInterceptors).excludePathPatterns(
                "/user/login",
                "/user/register",
                "/api/email/send-code",
                "/api/email/verify",
                "/article/home",
                "/search");
    }
}
