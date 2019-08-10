package com.leyou.order.service;

import com.leyou.common.utils.IdWorker;
import com.leyou.item.client.ItemClient;
import com.leyou.item.dto.SkuDTO;
import com.leyou.order.dto.CartDTO;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.entity.Order;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderLogisticsMapper;
import com.leyou.order.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderLogisticsMapper orderLogisticsMappere;

    @Autowired
    private ItemClient itemClient;

    @Autowired
    private IdWorker idWorker;
    /**
     * 创建订单
     *
     * @param orderDTO
     * @return
     */
    public Long createOrder(OrderDTO orderDTO) {
        //1.新增订单
        Order order = new Order();
        //1.1订单编号
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);
        //1.2订单金额相关
        List<CartDTO> carts = orderDTO.getCarts();
        Map<Long, Integer> numMap = carts.stream().collect(Collectors.toMap(CartDTO::getSkuId, CartDTO::getNum));
        List<Long> idList = carts.stream().map(CartDTO::getSkuId).collect(Collectors.toList());
        //1.2.1查询sku
        List<SkuDTO> skuList = itemClient.querySkuByIds(idList);
        //1.2.2计算总金额
        long total = 0;
        for (SkuDTO sku : skuList) {
            //计算金额
            int num = numMap.get(sku.getId());
            total += (sku.getPrice() * num);
        }
        //1.2.3赋值
        order.setTotalFee(total);
        order.setPaymentType(orderDTO.getPaymentType());
        order.setPostFee(0L);
        order.setActualFee(total + order.getPostFee());

        //新增订单详情

        //新增订单物流
        return null;
    }
}
