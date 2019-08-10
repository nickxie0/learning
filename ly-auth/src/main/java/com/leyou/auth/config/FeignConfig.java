package com.leyou.auth.config;

import com.leyou.auth.task.PrivilegeTokenHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Autowired
    private JwtProperties prop;

    @Autowired
    private PrivilegeTokenHolder tokenHolder;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new PrivilegeRequestInterceptor(prop, tokenHolder);
    }

    class PrivilegeRequestInterceptor implements RequestInterceptor {

        private JwtProperties prop;

        private PrivilegeTokenHolder tokenHolder;

        public PrivilegeRequestInterceptor(JwtProperties prop, PrivilegeTokenHolder tokenHolder) {
            this.prop = prop;
            this.tokenHolder = tokenHolder;
        }

        @Override
        public void apply(RequestTemplate requestTemplate) {
            requestTemplate.header(prop.getApp().getHeaderName(), tokenHolder.getToken());
        }
    }

}
