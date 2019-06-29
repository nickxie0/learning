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

    @GetMapping("params")
    public ResponseEntity<List<SpecParamDTO>> queryParamByGid(@RequestParam("gid") Long gid) {
        return ResponseEntity.ok(specService.queryParamByGid(gid));
    }
}
