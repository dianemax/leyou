package com.leyou.order.config;

import com.leyou.order.interceptors.UserInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器生效哦
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class MvcConfig implements WebMvcConfigurer{

    @Autowired
    private JwtProperties prop;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // new 是自己在创建对象，但是拦截器中要使用spring，如果使用spring，就不能自己创建，要用spring来创建
        registry.addInterceptor(new UserInterceptor(prop)).addPathPatterns("/order/**");//只拦截order下的路径，同时允许微信调用
    }
}
