package com.leyou.item.web;

import com.leyou.item.dto.CategoryDTO;
import com.leyou.item.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 根据父类目id查询子类目的集合
     * @param pid
     * @return
     */
    @GetMapping("/of/parent")
    public ResponseEntity<List<CategoryDTO>> queryCategoryByParentId(@RequestParam("pid") Long pid) {
        List<CategoryDTO> list = categoryService.queryCategoryByParentId(pid);
        return ResponseEntity.ok(list);
    }

    /**
     * 根据id批量查询分类
     * @param ids
     * @return
     */
    @GetMapping("list")
    public ResponseEntity<List<CategoryDTO>> queryCategoryByIds(@RequestParam("ids") List<Long> ids) {
        List<CategoryDTO> list = categoryService.queryCategoryByIds(ids);
        return ResponseEntity.ok(list);
    }
}
