package com.shop.domain.order.repository.custom;

import com.shop.application.order.dto.ItemSearch;
import com.shop.application.order.dto.MainItemDto;
import com.shop.domain.order.model.Item;

import java.util.List;


public interface ItemRepositoryCustom {

    List<Item> getAdminItemWithSearchCondition(ItemSearch itemSearch);
    List<MainItemDto> getMainItemPage(ItemSearch itemSearch);
}
