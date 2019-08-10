package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.constants.MQConstants;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.BeanHelper;
import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.*;
import com.leyou.item.entity.Sku;
import com.leyou.item.entity.Spu;
import com.leyou.item.entity.SpuDetail;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodService {

    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private SpuDetailMapper spuDetailMapper;
    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    /**
     * 分页查询商品
     * @param page
     * @param rows
     * @param saleable
     * @param key
     * @return
     */
    public PageResult<SpuDTO> querySpuByPage(Integer page, Integer rows, Boolean saleable, String key) {
        //分页
        PageHelper.startPage(page, rows);
        //条件过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //关键字过滤
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("name", "%" + key + "%");
        }
        //上下架过滤
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }
        //排序
        example.setOrderByClause("update_time DESC");
        //搜索
        List<Spu> spuList = spuMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(spuList)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        PageInfo<Spu> info = new PageInfo<>(spuList);

        //转换成DTO
        List<SpuDTO> spuDTOList = BeanHelper.copyWithCollection(spuList, SpuDTO.class);

        handleCategoryAndBrandName(spuDTOList);
        return new PageResult<>(info.getTotal(), info.getPages(), spuDTOList);
    }

    private void handleCategoryAndBrandName(List<SpuDTO> spuDTOList) {
        for (SpuDTO spuDTO : spuDTOList) {
            //处理分类名称
            List<Long> ids = spuDTO.getCategoryIds();
            String name = categoryService.queryCategoryByIds(ids)
                    .stream()
                    .map(CategoryDTO::getName)
                    .collect(Collectors.joining("/"));
            spuDTO.setCategoryName(name);

            //处理商品名称
            Long brandId = spuDTO.getBrandId();
            BrandDTO brandDTO = brandService.queryById(brandId);
            spuDTO.setBrandName(brandDTO.getName());



        }
    }

    /**
     * 新增商品
     *
     * @param spuDTO
     */
    @Transactional
    public void saveGoods(SpuDTO spuDTO) {
        //新增spu
        Spu spu = BeanHelper.copyProperties(spuDTO, Spu.class);
        //默认新增商品是下架
        spu.setSaleable(false);
        int count = spuMapper.insertSelective(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }

        //新增sputDetail
        SpuDetailDTO spuDetailDTO = spuDTO.getSpuDetail();
        SpuDetail spuDetail = BeanHelper.copyProperties(spuDetailDTO, SpuDetail.class);
        spuDetail.setSpuId(spu.getId());
        count = spuDetailMapper.insertSelective(spuDetail);
        if (count != 1) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
        insertSkuList(spuDTO.getSkus(), spu.getId());

    }
    //新增sku
    private void insertSkuList(List<SkuDTO> spuDTOList, Long spuId) {
        int count;
        List<Sku> skuList = BeanHelper.copyWithCollection(spuDTOList, Sku.class);
        for (Sku sku : skuList) {
            sku.setSpuId(spuId);
            //下架状态
            sku.setEnable(false);
        }
        count = skuMapper.insertList(skuList);
        if (count != skuList.size()) {
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }

    /**
     * 上架或下架
     * @param id
     * @param saleable
     */
    @Transactional
    public void updateSaleable(Long id, Boolean saleable) {
        //更新spu上下架
        Spu spu = new Spu();
        spu.setId(id);
        spu.setSaleable(saleable);
        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }

        //更新sku的上下架
        //设置更新字段
        Sku record = new Sku();
        record.setEnable(saleable);
        //更新的匹配条件
        Example example = new Example(Sku.class);
        example.createCriteria().andEqualTo("spuId", id);
        count = skuMapper.updateByExampleSelective(record, example);
        if (count <= 0) {
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }

        //发送消息
        String routingKey = saleable ? MQConstants.RoutingKey.ITEM_UP_KEY:MQConstants.RoutingKey.ITEM_DOWN_KEY;
        amqpTemplate.convertAndSend(
                MQConstants.Exchange.ITEM_EXCHANGE_NAME, routingKey, id);
    }



    /**
     * 根据spuId查询spuDetail
     * @param spuId
     * @return
     */
    public SpuDetailDTO queryDetailBySpuId(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        if (spuDetail == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return BeanHelper.copyProperties(spuDetail, SpuDetailDTO.class);
    }

    /**
     * 根据spuId查询sku
     * @param spuId
     * @return
     */
    public List<SkuDTO> querySkuBySpuId(Long spuId) {
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> list = skuMapper.select(sku);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(list, SkuDTO.class);
    }

    /**
     * 更新商品
     * @param spuDTO
     */
    @Transactional
    public void updateGoods(SpuDTO spuDTO) {
        //获取spu的id
        Long spuId = spuDTO.getId();
        if (spuId == null) {
            throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }
        //删除sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        //查询以前有几个sku
        int size = skuMapper.selectCount(sku);
        if (size > 0) {
            //删除
            int count = skuMapper.delete(sku);
            if (count != size) {
                throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
            }
        }
        //更新spu
        Spu spu = BeanHelper.copyProperties(spuDTO, Spu.class);
        spu.setSaleable(null);
        spu.setCreateTime(null);
        spu.setUpdateTime(null);
        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if (count != 1) {
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }

        //更新sputDetail
        SpuDetailDTO spuDetailDTO = spuDTO.getSpuDetail();
        SpuDetail spuDetail = BeanHelper.copyProperties(spuDetailDTO, SpuDetail.class);
        spuDetail.setSpuId(spu.getId());
        count = spuDetailMapper.updateByPrimaryKey(spuDetail);
        if (count != 1) {
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }

        //新增sku
        insertSkuList(spuDTO.getSkus(),spu.getId());
    }

    /**
     * 根据id查询spu
     * @param id
     * @return
     */
    public SpuDTO querySpuById(Long id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        SpuDTO spuDTO = BeanHelper.copyProperties(spu, SpuDTO.class);
        //查询detail
        spuDTO.setSpuDetail(queryDetailBySpuId(id));
        //查询skus
        spuDTO.setSkus(querySkuBySpuId(id));

        return spuDTO;
    }

    public List<SkuDTO> querySkuByIds(List<Long> ids) {
        List<Sku> list = skuMapper.selectByIdList(ids);
        if (CollectionUtils.isEmpty(list)) {
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(list, SkuDTO.class);
    }
}
