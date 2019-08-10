package com.leyou.common.utils;

import com.leyou.common.auth.entity.UserInfo;

public class UserHolder {
    private static ThreadLocal<UserInfo> tl = new ThreadLocal<>();

    public static void setUser(UserInfo user) {
        tl.set(user);
    }

    public static UserInfo getUser() {
        return tl.get();
    }

    public static void remove() {
        tl.remove();
    }
}
