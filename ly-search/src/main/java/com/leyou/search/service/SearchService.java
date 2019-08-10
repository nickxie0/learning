package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.client.ItemClient;
import com.leyou.item.dto.*;
import com.leyou.search.dto.GoodsDTO;
import com.leyou.search.dto.SearchRequest;
import com.leyou.search.pojo.Goods;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private ItemClient itemClient;

    public Goods buildGoods(SpuDTO spu) {
        // 1.关键字搜索的字段，包含名称、分类、品牌等等
        // 1.1.根据id查询品牌
        BrandDTO brand = itemClient.queryBrandById(spu.getBrandId());
        // 1.2.根据id查询分类
        String name = itemClient.queryCategoryByIds(spu.getCategoryIds())
                .stream().map(CategoryDTO::getName).collect(Collectors.joining(" "));
        // 1.3.拼接
        String all = spu.getName() + brand.getName() + name;

        // 2.spu下的sku的集合
        List<SkuDTO> skuList = CollectionUtils.isEmpty(spu.getSkus()) ? itemClient.querySkuBySpuId(spu.getId()) : spu.getSkus();
        List<Map<String, Object>> skuMap = new ArrayList<>();
        for (SkuDTO skuDTO : skuList) {
            Map<String, Object> sku = new HashMap<>();
            sku.put("id", skuDTO.getId());
            sku.put("title", skuDTO.getTitle());
            sku.put("price", skuDTO.getPrice());
            sku.put("image", StringUtils.substringBefore(skuDTO.getImages(), ","));
            skuMap.add(sku);
        }
        // 3.spu下的sku的价格集合
        Set<Long> price = skuList.stream().map(SkuDTO::getPrice).collect(Collectors.toSet());

        // 4.商品规格参数，包含规格参数名称和规格参数值
        Map<String, Object> specs = new HashMap<>();

        // 4.1.查询规格参数名称（tb_spec_param）
        List<SpecParamDTO> params = itemClient.querySpecParams(null, spu.getCid3(), true);

        // 4.2.查询规格值（tb_spu_detail)
        SpuDetailDTO detail = spu.getSpuDetail() == null ? itemClient.querySpuDetailById(spu.getId()) : spu.getSpuDetail();
        // 4.2.1.取出通用规格参数
        Map<Long, Object> genericSpec = JsonUtils.toMap(detail.getGenericSpec(), Long.class, Object.class);
        // 4.2.2.取出特有规格参数
        Map<Long, List<String>> specialSpec = JsonUtils.nativeRead(detail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {
        });

        // 4.3.封装数据
        for (SpecParamDTO param : params) {
            // 取出规格名称，作为specs的key
            String key = param.getName();
            // 规格值
            Object value = null;
            // 取出id，作为key去genericSpec或specialSpecs中读取值
            Long id = param.getId();
            // 判断是否是generic
            if (param.getGeneric()) {
                // 通用的，去genericSpec取
                value = genericSpec.get(id);
            } else {
                // 特有，去specialSpecs取
                value = specialSpec.get(id);
            }
            // 判断param是否是数字类型，是否有segment，如果有转成段来处理，
            if (param.getNumeric()) {
                // 是数字，需要分段
                value = chooseSegment(value, param);
            }
            specs.put(key, value);
        }

        // 5.创建Goods对象
        Goods goods = new Goods();
        goods.setSubTitle(spu.getSubTitle());
        goods.setId(spu.getId());
        goods.setCategoryId(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime().getTime());
        goods.setAll(all);
        goods.setPrice(price);
        goods.setSkus(JsonUtils.toString(skuMap));
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

    @Autowired
    private ElasticsearchTemplate esTemplate;


    /**
     * 查询分页商品列表
     * @param request
     * @return
     */
   public PageResult<GoodsDTO> search(SearchRequest request) {
       // 0.创建查询条件构建器
       NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
       //1.准备查询条件
       //1.1控制返回结果
       queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"},new String[0]));


       QueryBuilder basicQueryBuilder = buildBasicQuery(request);
       queryBuilder.withQuery(basicQueryBuilder);
       //分页
       int page = request.getPage() - 1;
       int size = request.getSize();
       queryBuilder.withPageable(PageRequest.of(page, size));

       //搜索结果
       AggregatedPage<Goods> result = esTemplate.queryForPage(queryBuilder.build(), Goods.class);

       //解析结果
       //分页数据
       long total = result.getTotalElements();
       int totalPages = result.getTotalPages();
       List<Goods> list = result.getContent();

       List<GoodsDTO> goodsDTOList = BeanHelper.copyWithCollection(list, GoodsDTO.class);

       return new PageResult<>(total, totalPages, goodsDTOList);
   }

    private QueryBuilder buildBasicQuery(SearchRequest request) {
        String key = request.getKey();
        if (StringUtils.isBlank(key)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        // 1.构建布尔查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 2.添加关键字查询
        queryBuilder.must(QueryBuilders.matchQuery("all", key).operator(Operator.AND));
        // 3.添加过滤条件
        Map<String, String> map = request.getFilter();
        if(!CollectionUtils.isEmpty(map)){
            // 循环取出每一对过滤项
            for (Map.Entry<String, String> entry : map.entrySet()) {
                // 取出过滤项名称
                String name = entry.getKey();
                // 处理名称
                if("品牌".equals(name)){
                    name = "brandId";
                }else if("分类".equals(name)){
                    name = "categoryId";
                }else{
                    name = "specs." + name;
                }
                // 取出过滤项的值
                String value = entry.getValue();
                // 添加到过滤条件中
                queryBuilder.filter(QueryBuilders.termQuery(name, value));
            }

        }
        return queryBuilder;
    }

    /**
     * 过滤项查询
     * @param request
     * @return
     */
    public Map<String, List<?>> querySearchFilter(SearchRequest request) {
        // 1.准备过滤项的map
        Map<String, List<?>> filterList = new LinkedHashMap<>();
        // 2.准备搜索过滤条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 2.0.控制返回的结果
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""}, new String[0]));
        // 2.1.搜索关键字
        QueryBuilder basicQueryBuilder = buildBasicQuery(request);

        queryBuilder.withQuery(basicQueryBuilder);

        // 2.2.分页，控制结果为1
        queryBuilder.withPageable(PageRequest.of(0, 1));

        // 3.添加分类和品牌聚合条件
        // 3.1.分类聚合
        queryBuilder.addAggregation(AggregationBuilders.terms("categoryAgg").field("categoryId"));
        // 3.2.品牌聚合
        queryBuilder.addAggregation(AggregationBuilders.terms("brandAgg").field("brandId"));

        // 4.获取聚合结果
        AggregatedPage<Goods> result = esTemplate.queryForPage(queryBuilder.build(), Goods.class);
        Aggregations aggs = result.getAggregations();
        // 4.1.处理品牌聚合
        Terms brandTerms = aggs.get("brandAgg");
        handleBrandAgg(brandTerms, filterList);
        // 4.2.处理分类聚合
        Terms categoryTerms = aggs.get("categoryAgg");
        List<Long> idList = handleCategoryAgg(categoryTerms, filterList);

        // 判断分类是否只剩下一个
        if (idList != null && idList.size() == 1) {
            handleSpecAgg(idList.get(0), basicQueryBuilder, filterList);
        }
        // 返回
        return filterList;
    }

    private void handleSpecAgg(Long cid, QueryBuilder basicQueryBuilder, Map<String, List<?>> filterList) {
        // 1、根据分类查询searching为true的规格
        List<SpecParamDTO> params = itemClient.querySpecParams(null, cid, true);

        // 2、准备搜索条件
        // 2.准备搜索过滤条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 2.0.控制返回的结果
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""}, new String[0]));
        // 2.1.搜索关键字
        queryBuilder.withQuery(basicQueryBuilder);
        // 2.2.分页，控制结果为1
        queryBuilder.withPageable(PageRequest.of(0, 1));

        // 3、对每一个规格都要添加聚合
        for (SpecParamDTO param : params) {
            // 取出参数名称，作为聚合名称
            String name = param.getName();
            queryBuilder.addAggregation(AggregationBuilders.terms(name).field("specs." + name));
        }

        // 4、搜索聚合结果
        AggregatedPage<Goods> result = esTemplate.queryForPage(queryBuilder.build(), Goods.class);
        // 5、解析聚合结果
        Aggregations aggs = result.getAggregations();
        System.out.println(filterList);
        for (SpecParamDTO param : params) {
            // 取出参数名称，作为聚合名称
            String name = param.getName();
            // 根据名称取出聚合
            Terms terms = aggs.get(name);
            // 取出桶中的数据
            List<String> list = terms.getBuckets().stream()
                    .map(b -> b.getKeyAsString())
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            // 存入map
            filterList.put(name, list);
        }
    }

    private List<Long> handleCategoryAgg(Terms terms, Map<String, List<?>> filterList) {
        // 获取id集合
        List<Long> idList = terms.getBuckets().stream()
                .map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());
        // 根据id查询
        List<CategoryDTO> categoryList = itemClient.queryCategoryByIds(idList);
        // 存入map
        filterList.put("分类", categoryList);
        return idList;
    }

    private void handleBrandAgg(Terms terms, Map<String, List<?>> filterList) {
        // 获取id集合
        List<Long> idList = terms.getBuckets().stream()
                .map(b -> b.getKeyAsNumber().longValue()).collect(Collectors.toList());
        // 根据id查询
        List<BrandDTO> brandList = itemClient.queryBrandByIds(idList);
        // 存入map
        filterList.put("品牌", brandList);
    }


    @Autowired
    private GoodsRepository goodsRepository;

    /**
     * 在索引库中新增
     * @param id
     */
    public void createIndex(Long id) {
        //查询spu
        SpuDTO spu = itemClient.querySpuById(id);
        //构建goods
        Goods goods = buildGoods(spu);

        //写入索引库
        goodsRepository.save(goods);
    }

    /**
     * 删除索引库中该商品
     * @param id
     */
    public void deleteById(Long id) {
        goodsRepository.deleteById(id);
    }
}
