package com.leyou.gateway.task;

import com.leyou.gateway.client.AuthClient;
import com.leyou.gateway.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PrivilegeTokenHolder {

    @Autowired
    private JwtProperties prop;

    private String token;

    /**
     * token刷新间隔
     */
    private static final long TOKEN_REFRESH_INTERVAL = 86400000L;

    /**
     * token获取失败后重试的间隔
     */
    private static final long TOKEN_RETRY_INTERVAL = 10000L;

    @Autowired
    private AuthClient authClient;

    /**
     * 定时任务，每隔指定时间，项目启动会先执行一次
     */
    @Scheduled(fixedDelay = TOKEN_REFRESH_INTERVAL)
    public void getTokenTask() throws InterruptedException {
        while (true) {
            try {
                //拉取token
                this.token = authClient.authorize(prop.getApp().getId(), prop.getApp().getSecret());
                //拉取token成功，结束循环
                log.info("网关拉取token成功，token:[{}]", token);
                break;
            } catch (Exception e) {
                log.info("网关拉取token失败，原因:{}", e.getMessage());
                //失败后，休眠一段时间，再次重试
                Thread.sleep(TOKEN_RETRY_INTERVAL);
            }
        }
    }

    public String getToken() {
        return token;
    }
}
