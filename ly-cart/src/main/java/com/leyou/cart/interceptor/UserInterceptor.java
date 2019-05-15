package com.leyou.cart.interceptor;

import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.cart.config.JwtProperties;
import com.leyou.common.utils.CookieUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 1 因为很多接口都需要进行登录，所以我们直接编写SpringMVC拦截器，进行统一登录校验
 *
 * 2 拦截器写完要让拦截器生效
 */
@Slf4j
public class UserInterceptor implements HandlerInterceptor {

    private JwtProperties prop;

    public UserInterceptor(JwtProperties prop) {
        this.prop = prop;
    }

    // threadLocal是一个map结构，key是thread，value是存储的值
    private static final ThreadLocal<UserInfo> tl = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        try {
            // 解析token -- 解析token要先获得cookie
            String token = CookieUtils.getCookieValue(request, prop.getCookieName());
            UserInfo user = JwtUtils.getInfoFromToken(token, prop.getPublicKey());

            // 保存user -- request和thread是“共享”的，所以可以把user放到这两个中
            tl.set(user); // key是不需要自己给定的，会自己获取

            return true;

        } catch (Exception e) {
            log.error("[购物车异常] 用户身份解析失败！", e);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        tl.remove();// 用完之后要删除
    }

    public static UserInfo getUser(){
        return tl.get();
    }
}
