package com.leyou.page.web;

import com.leyou.page.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Controller
public class PageController {

    @Autowired
    private PageService pageService;

    @GetMapping("item/{id}.html")
    public String toItemHtml(@PathVariable("id") Long spuId, Model model) {

        Map<String, Object> itemData = pageService.loadItemData(spuId);
        //添加模型数据
        model.addAllAttributes(itemData);
        return "item";
    }

    @GetMapping("hello")
    public String hello(Model model) {
        //添加模型数据
        model.addAttribute("msg", "hello,thymeleaf");
        //返回试图名称
        return "hello";
    }
}
