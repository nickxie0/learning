package com.leyou.auth.mapper;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationInfoMapperTest {

    @Autowired
    private ApplicationInfoMapper infoMapper;

    @org.junit.Test
    public void queryTargetIdList() {
        List<Long> list = infoMapper.queryTargetIdList(1L);
        System.out.println("list = " + list);
    }
}