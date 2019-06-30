package com.leyou.item.service;


import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.item.dto.CategoryDTO;
import com.leyou.item.entity.Category;
import com.leyou.item.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 根据父类目id查询子类目的集合
     *
     * @param pid
     * @return
     */
    public List<CategoryDTO> queryCategoryByParentId(Long pid) {
        //条件
        Category category = new Category();
        category.setParentId(pid);
        //查询
        List<Category> list = categoryMapper.select(category);
        //健壮性判断
        if (CollectionUtils.isEmpty(list)) {
            //结果为null  没有查到  返回404
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        //转换
        /*List<CategoryDTO> result = new ArrayList<>();
        for (Category c : list) {
            CategoryDTO categoryDTO = new CategoryDTO();
            categoryDTO.setId(c.getId());
            categoryDTO.setIsParent(c.getIsParent());
            categoryDTO.setName(c.getName());
            categoryDTO.setParentId(c.getParentId());
            categoryDTO.setSort(c.getSort());
        }*/
        List<CategoryDTO> result = BeanHelper.copyWithCollection(list, CategoryDTO.class);
        return result;
    }

    /**
     * 根据id集合查询categoryDTO
     * @param ids
     * @return
     */
    public List<CategoryDTO> queryCategoryByIds(List<Long> ids) {
        List<Category> list = categoryMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(list,CategoryDTO.class);
    }
}
