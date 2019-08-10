package com.leyou.gateway.filters;

import com.leyou.common.auth.entity.Payload;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.gateway.config.FilterProperties;
import com.leyou.gateway.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@Component
public class AuthFilter extends ZuulFilter {

    @Autowired
    private JwtProperties prop;

    @Autowired
    private FilterProperties filterProp;

    @Autowired
    private StringRedisTemplate redisTemplate;
    /**
     * 过滤器类型，前置类型
     * @return
     */
    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.FORM_BODY_WRAPPER_FILTER_ORDER + 1;
    }

    /**
     * true代表 下面的run方法会被执行，false：不执行run方法
     * @return
     */
    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        //获取request
        HttpServletRequest request = context.getRequest();
        //获取请求路径
        String path = request.getRequestURI();
        //判断path是否在白名单中  允许放行
        boolean isAllow = isAllowPath(path);
        //需要放行的路径这里返回false 需要拦截的路径，这里返回true
        return !isAllow;
    }

    private boolean isAllowPath(String path) {
        //获取白名单中的路径
        List<String> allowPaths = filterProp.getAllowPaths();
        //遍历白名单
        for (String allowPath : allowPaths) {
            //判断前缀是否匹配
            if (path.startsWith(allowPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤逻辑
     *
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        //获取request对象
        HttpServletRequest request = context.getRequest();
        //1.获取cookie中的token
        String token = CookieUtils.getCookieValue(request, prop.getUser().getCookieName());
        try {
            //2.解析token
            Payload<UserInfo> payload = JwtUtils.getInfoFromToken(token, prop.getPublicKey(), UserInfo.class);
            //3校验黑名单
            Boolean hasKey = redisTemplate.hasKey(payload.getId());
            if (hasKey != null && hasKey) {
                //证明存在于黑名单中，说明token已经退出过登录，无效
                throw new RuntimeException("token已经登出，是无效token");
            }
        } catch (Exception e) {
            //登录无效
            log.error("用户登录校验失败");
            //拦截用户请求
            context.setSendZuulResponse(false);
            context.setResponseStatusCode(403);  //Forbidden
        }
        return null;
    }
}
