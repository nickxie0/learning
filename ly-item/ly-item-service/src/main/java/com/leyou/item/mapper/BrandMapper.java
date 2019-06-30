package com.leyou.item.mapper;

import com.leyou.item.dto.BrandDTO;
import com.leyou.item.entity.Brand;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BrandMapper extends Mapper<Brand> {

    int insertCategoryBrand(@Param("id") Long id, @Param("ids") List<Long> ids);

    @Select("select b.id, b.name, b.letter, b.image from tb_category_brand cb inner join tb_brand b on b.id = cb.brand_id where cb.category_id = #{cid}")
    List<Brand> queryBrandByCid(@Param("cid") Long cid);
}
