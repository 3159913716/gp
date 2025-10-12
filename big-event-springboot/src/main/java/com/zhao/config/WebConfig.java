package com.zhao.config;

import com.zhao.interceptors.LoginInterceptors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//配置类,注册拦截器
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptors loginInterceptors;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录接口和注册接口不应当拦截
        registry.addInterceptor(loginInterceptors).excludePathPatterns(
                "/user/login",
                "/user/register",
                "/api/email/send-code",
                "/api/email/verify",
                "/article/home",
                "/search");
    }


//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        // 将 /images/** 映射到项目静态资源目录下的 images 文件夹
//        registry.addResourceHandler("/file/**")
//                .addResourceLocations("file:C:\\Code\\file\\");
//    }
}
