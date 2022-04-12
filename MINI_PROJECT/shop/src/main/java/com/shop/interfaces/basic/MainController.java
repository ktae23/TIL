package com.shop.interfaces.basic;

import com.shop.application.order.ItemService;
import com.shop.application.order.dto.ItemSearch;
import com.shop.application.order.dto.MainItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final ItemService itemService;


    @GetMapping("/")
    public String main(ItemSearch itemSearch, @RequestParam(required = false) Integer page, Model model) {
        final PageRequest pageRequest = PageRequest.of(Optional.ofNullable(page).orElse(0), 6);
        final Page<MainItemDto> items = itemService.getMainItemPage(itemSearch, pageRequest);
        model.addAttribute("items", items);
        model.addAttribute("itemSearch", itemSearch);
        model.addAttribute("maxPage", 5);
        return "main";
    }
}
