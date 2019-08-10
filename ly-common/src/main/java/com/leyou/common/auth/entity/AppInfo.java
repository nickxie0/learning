package com.leyou.common.auth.entity;

import lombok.Data;

import java.util.List;

/**
 * 微服务的部分载荷数据
 * @author 虎哥
 */
@Data
public class AppInfo {
    private Long id;
    private String serviceName;
    private List<Long> targetList;
}