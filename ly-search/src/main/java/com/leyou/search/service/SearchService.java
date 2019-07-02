package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.client.ItemClient;
import com.leyou.item.dto.*;
import com.leyou.search.pojo.Goods;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {


    @Autowired
    private ItemClient itemClient;

    /**
     * 提供一个封装Goods对象的方法
     * @param spuDTO
     * @return
     */
    public Goods buildGoods(SpuDTO spuDTO) {
        //创建Goods对象
        Goods goods = new Goods();
        goods.setSubTitle(spuDTO.getSubTitle());
        goods.setId(spuDTO.getId());
        goods.setCategoryId(spuDTO.getCid3());
        goods.setBrandId(spuDTO.getBrandId());
        goods.setCreateTime(spuDTO.getCreateTime().getTime());
        //1.关键字搜索的字段，包含名称，分类，品牌等
        //1.1 根据id查询品牌
        BrandDTO brandDTO = itemClient.queryBrandById(spuDTO.getBrandId());
        //1.2 根据id查询分类
        String name = itemClient.queryCategoryByIds(spuDTO.getCategoryIds()).stream().map(CategoryDTO::getName)
                .collect(Collectors.joining(" "));
        String all = spuDTO.getName() + brandDTO.getName() + name;
        goods.setAll(all);



        //spu下的sku的集合
        List<SkuDTO> skuDTOList = itemClient.querySkuBySpuId(spuDTO.getId());
        List<Map<String,Object>> skuMap = new ArrayList<>();
        for (SkuDTO skuDTO : skuDTOList) {
            Map<String, Object> sku = new HashMap<>();
            sku.put("id", skuDTO.getId());
            sku.put("title", skuDTO.getTitle());
            sku.put("price", skuDTO.getPrice());
            sku.put("image", StringUtils.substringBefore(skuDTO.getImages(), ","));
            skuMap.add(sku);
        }
        goods.setSkus(JsonUtils.toString(skuMap));



        //spu下的sku的价格集合
        Set<Long> price = skuDTOList.stream().map(SkuDTO::getPrice).collect(Collectors.toSet());
        goods.setPrice(price);


        //4.商品规格参数，包含规格参数名称和规格参数的值
        Map<String, Object> specs = new HashMap<>();
        //4.1查询规格参数的名称
        List<SpecParamDTO> params = itemClient.querySpecParams(null, spuDTO.getCid3(), true);
        //4.2查询规格参数的值
        SpuDetailDTO detail = itemClient.querySpuDetailById(spuDTO.getId());
        //4.2.1取出通用的规格参数
        Map<Long, Object> genericSpec = JsonUtils.toMap(detail.getGenericSpec(), Long.class, Object.class);
        //4.2.2取出特有的规格参数
        //Map<Long, List> specialSpec = JsonUtils.toMap(detail.getSpecialSpec(), Long.class, List.class);
        Map<Long, List<String>> specialSpec = JsonUtils.nativeRead(detail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {
        });
        //4.3封装数据
        for (SpecParamDTO param : params) {
            //取出规格名称，作为specs的key
            String key = param.getName();
            //规格值
            Object value = null;
            //取出id,作为key去genericSpec或SpecialSpec中读取值
            Long id = param.getId();
            //判断是否是generic
            if (param.getGeneric()) {
                //通用的，去genericSpec中找
                value = genericSpec.get(id);
            } else {
                //特有的，去specialSpec中找
                value = specialSpec.get(id);
            }
            //判断param是否是数字类型，是否有segment，如果有转成段处理
            if (param.getNumeric()) {
                //是数字，需要分段、
                value = chooseSegment(value,param);
            }
            specs.put(key, value);
        }
        goods.setSpecs(specs);

        return goods;
    }
    private String chooseSegment(Object value, SpecParamDTO p) {
        if (value == null || StringUtils.isBlank(value.toString())) {
            return "其它";
        }
        double val = parseDouble(value.toString());
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = parseDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = parseDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    private double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return 0;
        }
    }
}
