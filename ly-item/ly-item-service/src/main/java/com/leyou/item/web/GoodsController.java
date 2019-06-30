package com.leyou.item.web;

import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.SkuDTO;
import com.leyou.item.dto.SpuDTO;
import com.leyou.item.dto.SpuDetailDTO;
import com.leyou.item.service.GoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GoodsController {

    @Autowired
    private GoodService goodService;

    /**
     * 分页查询商品
     * @param key
     * @param rows
     * @param page
     * @param saleable
     * @return
     */
    @GetMapping("spu/page")
    public ResponseEntity<PageResult<SpuDTO>> querySpuByPage(
            @RequestParam(value = "key",required = false) String key,
            @RequestParam(value = "rows",defaultValue = "5") Integer rows,
            @RequestParam(value = "page",defaultValue = "1") Integer page,
            @RequestParam(value = "saleable",required = false) Boolean saleable
    ) {
        return ResponseEntity.ok(goodService.querySpuByPage(key, rows, page, saleable));
    }

    /**
     * 新增商品
     * @param spuDTO
     * @return
     */
    @PostMapping("goods")
    public ResponseEntity<Void> saveGoods(@RequestBody SpuDTO spuDTO) {
        goodService.saveGoods(spuDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 上架或下架功能
     *
     * @param id
     * @param saleable
     * @return
     */
    @PutMapping("spu/saleable")
    public ResponseEntity<Void> updateSaleable(@RequestParam("id") Long id, @RequestParam("saleable") Boolean saleable) {
        goodService.updateSaleable(id, saleable);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 根据spu的id查询spuDetail
     * @param spuId
     * @return
     */
    @GetMapping("spu/detail")
    public ResponseEntity<SpuDetailDTO> queryDetailBySpuId(@RequestParam("id") Long spuId) {
        return ResponseEntity.ok(goodService.queryDetailBySpuId(spuId));
    }

    /**
     * 根据spuId查询sku
     * @param spuId
     * @return
     */
    @GetMapping("sku/of/spu")
    public ResponseEntity<List<SkuDTO>> querySkuBySpuId(@RequestParam("id") Long spuId) {
        return ResponseEntity.ok(goodService.querySkuBySpuId(spuId));
    }

    /**
     * 更新商品
     * @param spuDTO
     * @return
     */
    @PutMapping("goods")
    public ResponseEntity<Void> updateGoods(@RequestBody SpuDTO spuDTO) {
        goodService.updateGoods(spuDTO);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
