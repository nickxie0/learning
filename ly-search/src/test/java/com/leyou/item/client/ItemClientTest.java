package com.leyou.item.client;

import com.leyou.item.dto.CategoryDTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ItemClientTest {

    @Autowired
    private ItemClient itemClient;

    @Test
    public void queryCategoryByParentIds() {
        List<CategoryDTO> list = itemClient.queryCategoryByIds(Arrays.asList(1L, 2L, 3L));
        for (CategoryDTO categoryDTO : list) {
            System.out.println("categoryDTO = " + categoryDTO);
        }
        Assert.assertEquals(3, list.size());
    }
}