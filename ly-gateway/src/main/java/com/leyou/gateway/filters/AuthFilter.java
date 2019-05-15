package com.leyou.gateway.filters;

import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.gateway.config.FilterProperties;
import com.leyou.gateway.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 获取cookie中的token
 * 通过JWT对token进行校验（只拿公钥）
 * 通过：则放行；不通过：则重定向到登录页
 */
@Component
@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
public class AuthFilter extends ZuulFilter{

    @Autowired
    private JwtProperties prop;

    @Autowired
    private FilterProperties filterProperties;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;//前置过滤器
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER - 1;//官方前置过滤器-1，可以把自己定义的过滤器放在官方过滤器之前
    }

    @Override
    public boolean shouldFilter() {

        // 获取上下文以及request
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        // 获取请求的URL路径
        String path = request.getRequestURI();

        // 判断是否在白名单内 如果在 则放行
        return !isAllowPath(path);//是否过滤
    }

    private boolean isAllowPath(String path) {

        List<String> allowPaths = filterProperties.getAllowPaths();
        for (String allowPath : allowPaths) {
            if(path.startsWith(allowPath)){
                return true;
            }
        }
        return false;
    }

    @Override
    public Object run() throws ZuulException {
        // 获取上下文 获取request
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        // 获取token
        String token = CookieUtils.getCookieValue(request, prop.getCookieName());

        // 解析token
        try {
            UserInfo user = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            //TODO 权限管理

        } catch (Exception e) {
            // 解析失败 未登录
            ctx.setSendZuulResponse(false);// 拦截功能
            ctx.setResponseStatusCode(403);// 返回状态码
        }

        return null;
    }
}
