package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.BrandDTO;
import com.leyou.item.entity.Brand;
import com.leyou.item.mapper.BrandMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.util.StringUtil;

import java.util.List;

@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;


    public PageResult<BrandDTO> queryBrandByPage(Integer page, Integer rows, String key, String sortBy, Boolean desc) {
        //分页
        PageHelper.startPage(page, rows);
        //过滤条件
        Example example = new Example(Brand.class);
        if (StringUtils.isNotBlank(key)) {
            example.createCriteria().orLike("name", "%" + key + "%")
                    .orEqualTo("letter", key.toUpperCase());
        }
        //排序
        if (StringUtils.isNotBlank(sortBy)) {
            String orderByClause = sortBy + (desc ? " DESC" : " ASC");
            example.setOrderByClause(orderByClause);// id desc
        }
        //根据条件查询
        List<Brand> brands = brandMapper.selectByExample(example);

        //判断是否为空
        if (CollectionUtils.isEmpty(brands)) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        //解析分页结果
        PageInfo<Brand> info = new PageInfo<>(brands);

        //转换为BrandDTO
        List<BrandDTO> list = BeanHelper.copyWithCollection(brands, BrandDTO.class);

        //返回结果
        return new PageResult<>(info.getTotal(),list);
    }

    /**
     * 新增品牌
     * @param brandDTO
     * @param ids
     */
    @Transactional
    public void saveBrand(BrandDTO brandDTO, List<Long> ids) {
        //新增品牌
        Brand brand = BeanHelper.copyProperties(brandDTO, Brand.class);
        int count = brandMapper.insertSelective(brand);
        if (count != 1) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
        //新增中间表
        Long id = brand.getId();
        //批量新增
        count = brandMapper.insertCategoryBrand(id, ids);
        if (count != ids.size()) {
            //新增失败，抛出异常
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }

    /**
     * 根据id查询BrandDTO
     * @param id
     * @return
     */
    public BrandDTO queryById(Long id) {
        Brand brand = brandMapper.selectByPrimaryKey(id);
        if (brand == null) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return BeanHelper.copyProperties(brand, BrandDTO.class);
    }

    /**
     * 根据分类id查询品牌
     * @param id
     * @return
     */
    public List<BrandDTO> querBrandByCid(Long id) {
        List<Brand> list = brandMapper.queryBrandByCid(id);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(list, BrandDTO.class);
    }
}
