package com.leyou.user.config;

import com.leyou.common.auth.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.PrivateKey;
import java.security.PublicKey;

@Slf4j
@Data
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties implements InitializingBean {

    /**
     * 公钥地址
     */
    private String pubKeyPath;

    private PublicKey publicKey;

    private AppProperties app = new AppProperties();

    @Data
    public class AppProperties {
        private int expire;
        private long id;
        private String serviceName;
        private String headerName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            // 获取公钥
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            log.error("【用户服务】初始化公钥和私钥失败！原因:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
