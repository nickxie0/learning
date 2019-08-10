package com.leyou.gateway.config;

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


    /**
     * 用户token相关属性
     */
    private UserProperties user = new UserProperties();

    @Data
    public class UserProperties {

        /**
         * 存放token的cookie名称
         */
        private String cookieName;
    }


    private AppProperties app = new AppProperties();

    @Data
    public class AppProperties {

        private long id;
        private String secret;

        private String headerName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            // 获取公钥和私钥
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            log.error("【网关】加载公钥失败！原因：{}", e);
            throw new RuntimeException(e);
        }
    }
}
