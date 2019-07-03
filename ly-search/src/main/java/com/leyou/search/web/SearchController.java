package com.leyou.search.web;

import com.leyou.common.vo.PageResult;
import com.leyou.search.dto.GoodsDTO;
import com.leyou.search.dto.SearchRequest;
import com.leyou.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SearchController {

    @Autowired
    private SearchService searchService;
    /**
     *搜索
     * @param request
     * @return
     */
    @PostMapping("page")
    public ResponseEntity<PageResult<GoodsDTO>> search(@RequestBody SearchRequest request) {
        return ResponseEntity.ok(searchService.search(request));
    }

    /**
     * 查询过滤项
     * @param request
     * @return
     */
    @PostMapping("filter")
    public ResponseEntity<Map<String, List<?>>> querySearchFilter(@RequestBody SearchRequest request) {
        return ResponseEntity.ok(searchService.querySearchFilter(request));
    }
}
