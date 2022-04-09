package com.shop.controller;

import com.shop.dto.ItemDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value = "/thymeleaf")
public class ThymeleafExController {

    @GetMapping(value = "/ex01")
    public String thymeleafExample01(Model model) {
        model.addAttribute("data", "타임리프 예제 입니다.");
        return "thymeleafEx/thymeleafEx01";
    }

    @GetMapping(value = "/ex02")
    public String thymeleafExample02(Model model) {
        ItemDto itemDto = ItemDto.builder()
                .itemName("테스트 상품1")
                .itemDetail("상품 상세 설명")
                .price(10000)
                .regTime(LocalDateTime.now())
                .build();
        model.addAttribute("itemDto", itemDto);
        return "thymeleafEx/thymeleafEx02";
    }

    @GetMapping(value = "/ex03")
    public String thymeleafExample03(Model model) {
        List<ItemDto> itemDtoList = new ArrayList<>() {{
            for (int i = 1; i <= 10; i++) {
                add(ItemDto.builder()
                        .itemName("테스트 상품" + i)
                        .itemDetail("상품 상세 설명" + i)
                        .price(1000 * i)
                        .regTime(LocalDateTime.now())
                        .build());
            }
        }};

        model.addAttribute("itemDtoList", itemDtoList);
        return "thymeleafEx/thymeleafEx03";
    }

    @GetMapping(value = "/ex04")
    public String thymeleafExample04(Model model) {
        List<ItemDto> itemDtoList = new ArrayList<>() {{
            for (int i = 1; i <= 10; i++) {
                add(ItemDto.builder()
                        .itemName("테스트 상품" + i)
                        .itemDetail("상품 상세 설명" + i)
                        .price(1000 * i)
                        .regTime(LocalDateTime.now())
                        .build());
            }
        }};
        model.addAttribute("itemDtoList", itemDtoList);
        return "thymeleafEx/thymeleafEx04";
    }

    @GetMapping(value = "/ex05")
    public String thymeleafExample05() {
        return "thymeleafEx/thymeleafEx05";
    }

    @GetMapping(value = "/ex06")
    public String thymeleafExample06(String param1, String param2, Model model) {
        model.addAttribute("param1", param1);
        model.addAttribute("param2", param2);
        return "thymeleafEx/thymeleafEx06";
    }

    @GetMapping()
    public String thymeleafExample07() {
        return "thymeleafEx/thymeleafEx07";
    }

}