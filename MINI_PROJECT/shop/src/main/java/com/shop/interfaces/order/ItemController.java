package com.shop.interfaces.order;

import com.shop.application.order.ItemService;
import com.shop.application.order.dto.ItemFormDto;
import com.shop.application.order.dto.ItemSearch;
import com.shop.domain.order.model.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.shop.infrastructure.utils.Utils.isNullOrEmpty;


@Slf4j
@RequestMapping("/admin")
@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping(value = {"/items"})
    public String itemManage(ItemSearch itemSearch, @RequestParam(required = false) Integer page, Model model) {
        final PageRequest pageRequest = PageRequest.of(Optional.ofNullable(page).orElse(0), 6);
        final Page<Item> items = itemService.getAdminItemPage(itemSearch, pageRequest);
        model.addAttribute("items", items);
        model.addAttribute("itemSearch", itemSearch);
        model.addAttribute("maxPage", 5);
        return "item/itemMng";
    }

    @GetMapping("/items/new")
    public String itemForm(Model model) {
        model.addAttribute("itemFormDto", new ItemFormDto());
        return "item/itemForm";
    }

    @PostMapping("/items/new")
    public String itemNew(@Valid ItemFormDto itemFormDto, BindingResult bindingResult, Model model,
                          @RequestPart("itemImgFile") List<MultipartFile> itemImgFileList) {
        if (bindingResult.hasErrors()) {
            return "item/itemForm";
        }

        if (isNullOrEmpty(itemImgFileList)) {
            model.addAttribute("errorMessage", "첫 번째 상품 이미지는 필수 입력 값입니다.");
            return "item/itemForm";
        }

        try {
            itemService.saveItem(itemFormDto, itemImgFileList);
        } catch (IOException e) {
            model.addAttribute("errorMessage", "상품 등록 중 에러가 발생했습니다.");
            return "item/itemForm";
        }

        return "redirect:/";
    }

    @GetMapping("/items/{itemId}")
    public String getItemDto(@PathVariable Long itemId, Model model) {
        try {
            final ItemFormDto itemFormDto = itemService.getItemDto(itemId);
            model.addAttribute("itemFormDto", itemFormDto);
        } catch (EntityNotFoundException e) {
            model.addAttribute("errorMessage", "존재하지 않는 상품 입니다.");
            model.addAttribute("itemFormDto", new ItemFormDto());
            return "item/itemForm";
        }
        return "item/itemForm";
    }

    @PostMapping("/items/{itemId}")
    public String itemUpdate(@Valid ItemFormDto itemFormDto, BindingResult bindingResult, Model model,
                             @PathVariable Long itemId,
                             @RequestPart("itemImgFile") List<MultipartFile> itemImgFileList) {
        if (bindingResult.hasErrors()) {
            return "item/itemForm";
        }

        if (!itemId.equals(itemFormDto.getId())) {
            model.addAttribute("errorMessage", "아이디 값이 일치하지 않습니다.");
            return "item/itemForm";
        }

        if (isNullOrEmpty(itemImgFileList)) {
            model.addAttribute("errorMessage", "첫 번째 상품 이미지는 필수 입력 값입니다.");
            return "item/itemForm";
        }

        try {
            itemService.updateItem(itemFormDto, itemImgFileList);
        } catch (IOException e) {
            model.addAttribute("errorMessage", "상품 수정 중 에러가 발생했습니다.");
            return "item/itemForm";
        }

        return "redirect:/";
    }
}
