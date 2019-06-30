package com.leyou.item.web;

import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.BrandDTO;
import com.leyou.item.entity.Brand;
import com.leyou.item.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 查询分页品牌
     * @param page
     * @param rows
     * @param key
     * @param sortBy
     * @param desc
     * @return
     */
    @GetMapping("page")
    public ResponseEntity<PageResult<BrandDTO>> queryBrandByPage(
            @RequestParam(value = "page", defaultValue = "1")Integer page,
            @RequestParam(value = "rows", defaultValue = "5")Integer rows,
            @RequestParam(value = "key", required = false)String key,
            @RequestParam(value = "sortBy", required = false)String sortBy,
            @RequestParam(value = "desc", defaultValue = "false")Boolean desc) {
         PageResult<BrandDTO> pageList = brandService.queryBrandByPage(page,rows,key,sortBy,desc);
        return ResponseEntity.ok(pageList);
    }

    /**
     * 新增品牌
     * @param brand
     * @param ids
     * @return
     */
    @PostMapping
    public ResponseEntity<Void> saveBrand(BrandDTO brand, @RequestParam("cids")List<Long> ids) {
        brandService.saveBrand(brand,ids);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 根据分类id查询品牌
     * @param id
     * @return
     */
    @GetMapping("/of/category")
    public ResponseEntity<List<BrandDTO>> queryBrandByCid(@RequestParam("id") Long id) {
        return ResponseEntity.ok(brandService.querBrandByCid(id));
    }

}
