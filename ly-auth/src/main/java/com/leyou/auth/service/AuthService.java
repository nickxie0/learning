package com.leyou.auth.service;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.entity.AppInfo;
import com.leyou.auth.entity.ApplicationInfo;
import com.leyou.auth.mapper.ApplicationInfoMapper;
import com.leyou.common.auth.entity.Payload;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.CookieUtils;
import com.leyou.user.client.UserClient;
import com.leyou.user.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties prop;

    public void login(String username, String password, HttpServletResponse response) {
        try {
            //拿用户名和密码去用户中心查询
            UserDTO user = userClient.queryUserByUserNameAndPassword(username, password);

            //判断是否查到
            if (user == null) {
                throw new RuntimeException("用户名或密码错误");
            }
            //如果有，利用私钥，生成token
            UserInfo userInfo = new UserInfo(user.getId(), user.getUsername(), "GUEST");
            //生成token
            String token = JwtUtils.generateTokenExpireInMinutes(userInfo, prop.getPrivateKey(), prop.getUser().getExpire());

            //把token返回给用户,写入cookie
            writeCookie(response, token);
        } catch (Exception e) {
            log.error("用户登录失败，原因：{}", e.getMessage());
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }

    }

    private void writeCookie(HttpServletResponse response, String token) {
        CookieUtils.newCookieBuilder()
                .name(prop.getUser().getCookieName()).value(token)
                .domain(prop.getUser().getCookieDomain())
                .httpOnly(true)
                .response(response)
                .build();
    }

    /**
     * 校验用户是否登录
     * @param request
     * @param response
     * @return
     */
    public UserInfo verify(HttpServletRequest request, HttpServletResponse response) {
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
            //4.token续期
            //4.1取出过期时间
            Date expiration = payload.getExpiration();
            // 4.2获取剩余有效时间
            long remainTime = expiration.getTime() - System.currentTimeMillis();
            //4.3判断token过期时间距离现在是否小于10分钟
            if (remainTime < prop.getUser().getMaxRemainTime()) {
                //如果小于，则重新生成token
                String newToken = JwtUtils.generateTokenExpireInMinutes(
                        payload.getUserInfo(), prop.getPrivateKey(), prop.getUser().getExpire());

                //把token重新写入cookie
                writeCookie(response, newToken);
            }
            //返回用户信息
            return payload.getUserInfo();
        } catch (Exception e) {
            log.error("校验登录状态失败，原因：{}", e.getMessage(), e);
            //token无效或者过期
            throw new LyException(ExceptionEnum.UNAUTHORIZED, e);
        }
    }

    @Autowired
    private StringRedisTemplate redisTemplate;
    /**
     * 退出登录
     * @param request
     * @param response
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        //获取cookie中token
        String token = CookieUtils.getCookieValue(request, prop.getUser().getCookieName());
        //解析token，获取过期时间，获取id
        Payload<Object> payload = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
        //获取过期时间
        Date expiration = payload.getExpiration();
        //获取id
        String id = payload.getId();
        //获取剩余有效时间
        long remainTime = expiration.getTime() - System.currentTimeMillis();
        //记录id到redis中，设置有效期为token的剩余有效期
        redisTemplate.opsForValue().set(id, "1", remainTime, TimeUnit.MILLISECONDS);

        //删除cookie
        CookieUtils.deleteCookie(prop.getUser().getCookieName(),prop.getUser().getCookieDomain()
        ,response);
    }

    @Autowired
    private ApplicationInfoMapper infoMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    /**
     * 微服务授权
     *
     * @param id
     * @param secret
     * @return
     */
    public String authorize(Long id, String secret) {
        try {
            //1.校验服务身份
            //1.1根据id查询applicationInfo
            ApplicationInfo app = infoMapper.selectByPrimaryKey(id);
            //1.2判断是否查到
            if (app == null) {
                //id或密码错误
                throw new RuntimeException("服务Id或密码错误");
            }
            //校验secret（passwordEncode）
            if (!passwordEncoder.matches(secret, app.getSecret())) {
                //id或密码错误
                throw new RuntimeException("服务Id或密码错误");
            }
            //2.生成Jwt
            //2.1查询服务的权限
            List<Long> targetIdList = infoMapper.queryTargetIdList(app.getId());

            // 2.1生成载荷
            AppInfo appInfo = new AppInfo();
            appInfo.setId(app.getId());
            appInfo.setServiceName(app.getServiceName());
            appInfo.setTargetList(targetIdList);
            //2。2生成token
            return JwtUtils.generateTokenExpireInMinutes(appInfo, prop.getPrivateKey(), prop.getApp().getExpire());
        } catch (RuntimeException e) {
            log.error("服务授权认证失败，原因:{}", e.getMessage(), e);
            throw new LyException(ExceptionEnum.UNAUTHORIZED, e);
        }
    }
}
