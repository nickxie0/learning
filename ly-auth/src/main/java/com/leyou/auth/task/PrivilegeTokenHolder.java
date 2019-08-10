package com.leyou.auth.task;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.entity.AppInfo;
import com.leyou.auth.mapper.ApplicationInfoMapper;
import com.leyou.common.auth.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PrivilegeTokenHolder {

    @Autowired
    private JwtProperties prop;

    private String token;

    @Autowired
    private ApplicationInfoMapper infoMapper;
    /**
     * token刷新间隔
     */
    private static final long TOKEN_REFRESH_INTERVAL = 86400000L;

    /**
     * token获取失败后重试的间隔
     */
    private static final long TOKEN_RETRY_INTERVAL = 10000L;


    /**
     * 定时任务，每隔指定时间，项目启动会先执行一次
     */
    @Scheduled(fixedDelay = TOKEN_REFRESH_INTERVAL)
    public void getTokenTask() throws InterruptedException {
        while (true) {
            try {
                List<Long> targetList = infoMapper.queryTargetIdList(prop.getApp().getId());
                AppInfo appInfo = new AppInfo();
                appInfo.setId(prop.getApp().getId());
                appInfo.setServiceName(prop.getApp().getServiceName());
                appInfo.setTargetList(targetList);
                //生成token
                this.token = JwtUtils.generateTokenExpireInMinutes(appInfo, prop.getPrivateKey(), prop.getApp().getExpire());
                //生成token成功，结束循环
                log.info("授权中心定时生成token成功，token:[{}]", token);
                break;
            } catch (Exception e) {
                log.info("授权中心定时生成token失败，原因:{}", e.getMessage());
                //失败后，休眠一段时间，再次重试
                Thread.sleep(TOKEN_RETRY_INTERVAL);
            }
        }
    }

    public String getToken() {
        return token;
    }
}
