package com.leyou.user.interceptors;

import com.leyou.common.auth.entity.AppInfo;
import com.leyou.common.auth.entity.Payload;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.user.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 访问者的jwt校验
 */
@Slf4j
public class PrivilegeHandlerInterceptor implements HandlerInterceptor {


    private JwtProperties prop;

    public PrivilegeHandlerInterceptor(JwtProperties prop) {
        this.prop = prop;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            //获取JWT
            String token = request.getHeader(prop.getApp().getHeaderName());
            //获取载荷
            Payload<AppInfo> payload = JwtUtils.getInfoFromToken(token, prop.getPublicKey(), AppInfo.class);

            //获取载荷中的AppInfo，获取其权限信息
            AppInfo appInfo = payload.getUserInfo();
            List<Long> targetList = appInfo.getTargetList();
            //判断权限信息中，是否有当前服务的id
            if (!targetList.contains(prop.getApp().getId())) {
                //说明其无权限
                throw new RuntimeException("没有访问权限");
            }
            log.info("服务{},正在请求接口:{}", appInfo.getServiceName(), request.getRequestURI());
            return true;
        } catch (RuntimeException e) {
            log.error("非法访问，原因:{}", e.getMessage());
            //解析失败，说明没有权限,抛出异常
            throw new LyException(ExceptionEnum.UNAUTHORIZED, e);
        }
    }


}
