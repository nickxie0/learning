package com.leyou.user.client;

import com.leyou.user.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("user-service")
public interface UserClient {

    /**
     * 根据用户和密码查询用户
     * @param username
     * @param password
     * @return
     */
    @GetMapping("query")
    UserDTO queryUserByUserNameAndPassword(
            @RequestParam("username") String username,
            @RequestParam("password") String password) ;


}
