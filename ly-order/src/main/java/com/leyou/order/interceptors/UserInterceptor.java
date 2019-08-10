package com.leyou.order.interceptors;

import com.leyou.common.auth.entity.Payload;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.CookieUtils;
import com.leyou.common.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;

@Slf4j
public class UserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取token
        String token = CookieUtils.getCookieValue(request, "LY_TOKEN");
        try {
            //解析用户信息
            Payload<UserInfo> payload = JwtUtils.getInfoFromToken(token, UserInfo.class);
            //获取其中用户
            UserInfo user = payload.getUserInfo();
            UserHolder.setUser(user);
            //放行
            return true;
        } catch (UnsupportedEncodingException e) {
            //解析用户信息失败
            log.error("解析用户失败，原因:{}", e.getMessage());
            throw new LyException(ExceptionEnum.UNAUTHORIZED, e);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.remove();
    }
}
