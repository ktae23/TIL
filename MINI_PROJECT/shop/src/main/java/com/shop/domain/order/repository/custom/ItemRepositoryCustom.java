package com.shop.domain.order.repository.custom;

import com.shop.application.order.dto.ItemSearch;
import com.shop.domain.order.model.Item;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface ItemRepositoryCustom {

    List<Item> getAdminItemWithSearchCondition(ItemSearch itemSearch, Pageable pageable);
}
