package com.shop.interfaces.order;

import com.shop.application.order.ItemService;
import com.shop.application.order.dto.ItemFormDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items/{itemId}")
    public String itemDtl(@PathVariable Long itemId, Model model) {
        final ItemFormDto itemFormDto = itemService.getItemDto(itemId);
        model.addAttribute("item", itemFormDto);
        return "item/itemDtl";
    }
}
