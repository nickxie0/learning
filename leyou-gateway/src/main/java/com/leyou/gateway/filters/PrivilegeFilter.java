package com.leyou.gateway.filters;

import com.leyou.gateway.config.JwtProperties;
import com.leyou.gateway.task.PrivilegeTokenHolder;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;


/**
 * 在服务调用者发起请求之前，拦截请求，携带JWT
 */
@Component
public class PrivilegeFilter extends ZuulFilter {


    @Autowired
    private PrivilegeTokenHolder tokenHolder;

    @Autowired
    private JwtProperties prop;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER + 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    /**
     * 添加jwt到请求头中
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {
        //1.获取jwt
        String token = tokenHolder.getToken();
        //2.获取上下文，获取request
        RequestContext ctx = RequestContext.getCurrentContext();
        //3.添加头信息
        ctx.addZuulRequestHeader(prop.getApp().getHeaderName(), token);

        return null;
    }
}
