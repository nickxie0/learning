package com.leyou.page.service;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.item.client.ItemClient;
import com.leyou.item.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PageService {

    @Autowired
    private ItemClient itemClient;

    public Map<String, Object> loadItemData(Long spuId) {

        //查询需要的数据
        //查询spu
        SpuDTO spu = itemClient.querySpuById(spuId);
        //查询分类
        List<CategoryDTO> categories = itemClient.queryCategoryByIds(spu.getCategoryIds());

        //查询品牌
        BrandDTO brand = itemClient.queryBrandById(spu.getBrandId());
        //查询specs
        List<SpecGroupDTO> specs = itemClient.querySpecs(spu.getCid3());
        //封装一个map，填写商品数据
        Map<String, Object> data = new HashMap<>();
        data.put("categories",categories);
        data.put("brand",brand);
        data.put("spuName",spu.getName());
        data.put("subTitle",spu.getSubTitle());
        data.put("detail",spu.getSpuDetail());
        data.put("skus",spu.getSkus());
        data.put("specs",specs);

        return data;
    }

    @Autowired
    private SpringTemplateEngine templateEngine;

    private static final String HTML_DIR = "D:\\java\\nginx-1.14.0\\html\\item";

    public void createHtml(Long id) {

        //准备上下文
        Context context = new Context();
        context.setVariables(loadItemData(id));
        //准备文件路径
        File file = new File(HTML_DIR, id + ".html");
        //准备流

        try (PrintWriter writer = new PrintWriter(file, "UTF-8");) {
            templateEngine.process("item", context, writer);
        } catch (Exception e) {
            log.error("生成静态页面失败！原因:{}", e.getMessage(), e);
            throw new LyException(ExceptionEnum.FILE_WRITER_ERROR, e);
        }

    }

    public void deleteItemHtml(Long id) {
        //准备文件路径
        File file = new File(HTML_DIR, id + ".html");
        if (file.exists()) {
            boolean result = file.delete();
            if (!result) {
                throw new LyException(ExceptionEnum.DELETE_OPERATION_FAIL);
            }
        }
    }
}
