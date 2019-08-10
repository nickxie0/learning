package com.leyou.common.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    /**
     * 用户id
     */
    private Long id;
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户角色：普通用户？管理员
     */
    private String role;
}