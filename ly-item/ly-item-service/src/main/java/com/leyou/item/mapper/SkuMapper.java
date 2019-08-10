package com.leyou.item.mapper;

import com.leyou.common.mappers.BaseMapper;
import com.leyou.item.entity.Sku;
import tk.mybatis.mapper.common.special.InsertListMapper;

public interface SkuMapper extends BaseMapper<Sku>, InsertListMapper<Sku> {
}
