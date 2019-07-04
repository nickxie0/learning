package com.leyou.item.web;

import com.leyou.item.dto.SpecGroupDTO;
import com.leyou.item.dto.SpecParamDTO;
import com.leyou.item.service.SpecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("spec")
public class SpecController {
    @Autowired
    private SpecService specService;

    /**
     * 根据分类id查询规格组
     * @param cid
     * @return
     */
    @GetMapping("/groups/of/category")
    public ResponseEntity<List<SpecGroupDTO>> querySpecGroupByCid(@RequestParam("id") Long cid) {
        return ResponseEntity.ok(specService.querySpecGroupByCid(cid));
    }

    /**
     * 根据id查询规格参数
     *
     * @param gid
     * @param cid
     * @return
     */
    @GetMapping("params")
    public ResponseEntity<List<SpecParamDTO>> querySpecParams(
            @RequestParam(value = "gid", required = false) Long gid,
            @RequestParam(value = "cid", required = false) Long cid,
            @RequestParam(value = "searching", required = false) Boolean searching
    ) {
        return ResponseEntity.ok(specService.querySpecParams(gid, cid, searching));
    }



    /**
     * 根据分类id查询规格组及组内参数
     * @param id
     * @return
     */
    @GetMapping("list")
    public ResponseEntity<List<SpecGroupDTO>> querySpecs(@RequestParam("id") Long id){
        return ResponseEntity.ok(specService.querySpecs(id));
    }

}
