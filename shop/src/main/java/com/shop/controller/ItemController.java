package com.shop.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RequestMapping("/admin")
@Controller
@RequiredArgsConstructor
public class ItemController {

    @GetMapping("/item/new")
    public String itemForm(){
        return "/item/itemForm";
    }
}
