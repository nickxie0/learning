package com.leyou.service;

import com.leyou.common.exceptions.LyException;
import com.leyou.item.dto.Item;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class ItemService {
    public Item saveItem(Item item) {
        if (item.getPrice() == null) {
            throw new LyException(400,"价格不能为空");
        }
        int id = new Random().nextInt(100);
        item.setId(id);
        return item;
    }
}
